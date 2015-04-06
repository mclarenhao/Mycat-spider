/**
 *
 */
package turbo.crawler.concurrent

import java.util.concurrent.locks.ReentrantLock

/**
 * Turbo thread 线程池内受管线程
 *
 * @author mclaren
 *
 */
case class TurboThread(pool: TurboThreadPool, name: String, threadGroup: ThreadGroup) extends Thread {
  /**
   * 分配在该线程上的任务
   */
  private var attachedTask: ConcurrentTask = null

  private var running = true

  /**
   * 接受任务
   */
  def offerTask(attachedTask: ConcurrentTask): Unit = this.synchronized {
    this.attachedTask = attachedTask
    this.notifyAll
  }

  def shutdown = this.synchronized {
    this.running = false
    (this.notify())
  }
  /**
   * 判断当前是否空闲可用
   */
  def isAvaliable = (running && this.attachedTask == null)
  /**
   * 线程启动入口
   */
  override def run = this.synchronized {
    while (running) {
      if (attachedTask == null) {
        /**
         * 如果当前没有任何任务绑定在本线程上,则线程处于休眠状态以避免耗费CPU资源
         */
        this.synchronized(this.wait)
      }

      /**
       * 正式负载用户任务开始运行
       */
      if (this.attachedTask != null) {
        try {
          this.attachedTask.runnable.run()
        } catch {
          case e: Throwable => this.attachedTask.errHandler(e)
        }
        //触发线程池事件：描述本线程已经空闲
      }
      this.attachedTask = null
      this.pool.returnPool(this)
    }
  }
}