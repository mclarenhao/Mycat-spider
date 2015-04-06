package turbo.crawler.concurrent

import turbo.crawler.Logable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.Collections
import java.util.ArrayList
import java.util.concurrent.locks.ReentrantLock

/**
 * TURBO Thread Pool
 */
object defaultThreadGroup {
  val get = new ThreadGroup("turbo-thread-group")
}

class TurboThreadPool(maxCapacity: Int = 100, threadGroup: ThreadGroup = defaultThreadGroup.get) extends Logable {
  /**
   * 存放所有受管线程的上下文缓冲区
   */
  private val threadContext = Collections.synchronizedList(new ArrayList[TurboThread])

  private val freeThreadsQueue = new ConcurrentLinkedQueue[TurboThread]

  private var emptyCount = 0
  /**
   * 启动线程池
   */
  def start = {
    logger.info("正在启动线程,容量为:[{}]", maxCapacity)
    for (i <- 1 to maxCapacity) {
      var mt = new TurboThread(this, "TURBO_THREAD[" + i + "]", threadGroup)
      logger.debug("初始化受管线程{}", mt)
      mt.start
      freeThreadsQueue.add(mt)
      threadContext.add(mt)
    }
    logger.info("线程池启动就绪")
  }

  def returnPool(thread: TurboThread) {
    logger.debug("线程{}返回线程池", thread)
    freeThreadsQueue.add(thread)
    this.synchronized(this.notifyAll())
  }
  /**
   * 返回当前空闲的某个线程,如果没有任何线程处于空闲，则对来访者进行阻塞,直到有空闲线程为止
   */
  private def getAvailableThread: TurboThread = {
    //    for (i <- 0 until threadContext.size) {
    //      var t = threadContext.get(i)
    //      if (t.isAvaliable) {
    //        return t
    //      }
    //    }

    var t = freeThreadsQueue.poll
    if (t != null) return t
    this.synchronized(this.wait(10000))
    /**
     * 解除阻塞后,递归重新获取,根据CPU抢占策略，谁先抢到谁走,抢不到的继续等待一个回合
     */
    logger.info("重试:::::::")
    getAvailableThread
  }
  /**
   * 接受用户提交任务
   */
  def submitTask(task: ConcurrentTask): Unit = this.getAvailableThread.offerTask(task)

  /**
   * 无条件立即关闭
   */
  def shutdownImmediatly = {
    for (i <- 0 until threadContext.size) {
      threadContext.get(i).shutdown
    }

    logger.info("线程池已经关闭")
  }

  private def getAvailableThreadCounts = freeThreadsQueue.size
  /**
   * 从容关闭线程池：策略：当线程池内线程全部处于空闲状态累积超过10秒时,则对线程池进行关闭
   */
  def waiting4Shutdown = {
    try {
      while (true) {
        var ac = getAvailableThreadCounts
        if (ac == this.maxCapacity) emptyCount = emptyCount + 1 else 0
        logger.info("Free threads {}, Terminate least {} seconds", ac, 10 - emptyCount)
        if (emptyCount >= 10) throw new RuntimeException("Terminate")
        Thread.sleep(1000)
      }
    } catch {
      case e: Throwable => this.shutdownImmediatly
    }
  }
}