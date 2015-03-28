package turbo.crawler.power.v2

import java.util.concurrent.ScheduledThreadPoolExecutor
import turbo.crawler.{ Logable, StringAdapter }
import java.util.Hashtable
import turbo.crawler.power.Evt
import turbo.crawler.power.MessageDriven
import turbo.crawler.Lifecycle
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.LinkedList
import java.util.Collections
import java.util.concurrent.Executors

/**
 * 事件分发管理器
 * @author mclaren
 */
object EventManagerEx extends Logable with StringAdapter with Lifecycle with MessageDriven {
  /**
   * 内部线程池
   */
  private val exec = new ScheduledThreadPoolExecutor(sysprop("fetch.threads", "100").toInt)

  private var nullcount = 0

  private val eventQueue = new LinkedList[Any]()

  /**
   * 获取JVM配置参数
   */
  private def sysprop(key: String, default: String) = {
    var matched = System.getProperty(key)
    if (isNotEmpty(matched)) matched else default
  }

  /**
   * 销毁系统
   */
  override def shutdown = {
    try {
      while (true) {
        /**
         *  如果一分钟之内没有任何任务进入线程池则线程池就可宣布结束，故而程序员在开发任务时，进入线程池的吞吐量一定要控制在每分钟不少于一条
         */
        Thread.sleep(1000)
        if (exec.getActiveCount == 0) {
          nullcount = nullcount + 1
        } else {
          nullcount = 0
        }
        if (nullcount > 10) {
          throw new RuntimeException("Terminate")
        }
      }
    } catch {
      case e: Exception => {
        exec.shutdown()
        logger.info("Fetch completed and shutdown concurrenty fetchers.")
      }
    }

  }

  /**
   * 用户API: 通过receive函数来摘取系统事件并进行处理（但用户的处理程序被负载在内部线程池中）
   */
  def receive(fn: PartialFunction[Any, Unit]): Unit = {
    var t = new Thread(new Runnable() {
      val lock = new Object

      def availableTask(): Any = {
        try {
          eventQueue.pop
        } catch {
          case e: Exception => null
        }
      }

      def run = {
        while (true) {
          dispatchEventConcurrently(availableTask(), e => {
            try {
              fn(e)
            } catch {
              //当出现异常时递归产生消息供客户处理
              case ex: Exception => dispatchEventConcurrently(ex, m => fn(m))
            }
          })
        }
      }
    })
    t.setDaemon(true)
    t.start
  }

  /**
   * 并行分发事件
   */
  private def dispatchEventConcurrently(evt: Any, f: Any => Unit) = {
    if (evt != null) {
      var task = new FutureTask[Unit](new Callable[Unit]() {
        def call: Unit = f(evt)
      })
      this.exec.submit(task)
    }
  }

  /**
   * 处理事件分发
   */
  override def fireEvent(evt: Any): Unit = {
    /**
     * 采用偏函数的方式由用户自定义事件处理
     */
    eventQueue.add(evt)
  }

  private case class WrapList[T](list: java.util.List[T]) {
    def foreach(f: T => Unit) = for (i <- 0 to list.size() - 1) f(list.get(i))
  }
}