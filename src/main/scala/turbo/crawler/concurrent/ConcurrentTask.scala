package turbo.crawler.concurrent

import turbo.crawler.Logable

/**
 * 并发任务
 */
case class ConcurrentTask(runnable: Runnable /* 任务主体 */ , errHandler: Throwable => Unit = e => e.printStackTrace()) extends Logable