/**
 * @(#) Logable.scala 2015年3月4日
 * TURBO CRAWLER高性能网络爬虫
 */
package turbo.crawler

import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Logger

import org.slf4j.LoggerFactory

/**
 * 日志记录能力描述
 * @author mclaren
 */
trait Logable {
  def logger = ConsoleLogger(this.getClass.getName)
  /**
   * 获取日志记录操作接口
   */
  def logging(target: Class[_]) = LoggerFactory.getLogger(target)
}

object current {
  val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def now = format.format(new Date())
}
case class ConsoleLogger(name: String) {
  import current.now
  def getStack = {
    val e = new RuntimeException
    val arr = e.getStackTrace
    val stackElement = arr(2)
    stackElement.getFileName + ":" + stackElement.getLineNumber
  }

  def info(msg: Any, param: AnyRef*) = {
    println("[INFO](" + now + ")" + getStack + " - " + msg)
  }

  def warn(msg: Any, param: Any*) = {
    println("[WARN](" + now + ")" + getStack + " - " + msg)
  }

  def error(msg: Any, param: Any*) = {
    println("[ERROR](" + now + ")" + getStack + " - " + msg)
  }

  def error(msg: Any, e: Throwable) = {
    println("[ERROR](" + now + ")" + getStack + " - " + msg + ",caused by:" + e)
  }
}