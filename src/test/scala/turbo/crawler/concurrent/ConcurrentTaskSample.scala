package turbo.crawler.concurrent

import turbo.crawler.Logable

object ConcurrentTaskSample extends App with Logable {
  val pool = new TurboThreadPool(1000)
  pool.start
  var count = 0
  for (i <- 0 until 1000000) {
    pool.submitTask(new ConcurrentTask(new Runnable() {
      def run = ConcurrentTaskSample.synchronized {
        logger.info("Hello world {}", count)
        (count = count + 1)
      }
    }))
  }

  pool.waiting4Shutdown
  logger.info("VALUE IS " + count)
}