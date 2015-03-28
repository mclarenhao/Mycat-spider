package turbo.crawler.sample

import turbo.crawler.power.v2.EventManagerEx.{ receive, shutdown, fireEvent }
import turbo.crawler.power.Evt
import turbo.crawler.Logable
import turbo.crawler.io.LocalIO
import turbo.crawler.db.db
import turbo.crawler.power.v2.EventDrivenFetcherV2
import org.w3c.dom.Document
import turbo.crawler.StringAdapter
import turbo.crawler.JQSupport
import java.util.Date
import turbo.crawler.power.Evt
import turbo.crawler.power.Evt
import turbo.crawler.Fetchable

case class url(value: String)
object SampleCrawlerOfAutoHomePowerEx extends Logable with LocalIO with StringAdapter with JQSupport {
  val rdbms = db("default-ds")

  val fetcher = new EventDrivenFetcherV2[AutoBrand]()

  val now = new Date
  /**
   * 汽车之家网站采用最新事件调度框架DEMO
   */
  def main(args: Array[String]): Unit = {
    receive {
      case url: String => fetcher.fetch(url, x => x, parse _)(x => false)((x, y) => ())()
      case Evt(id, source) => {
        source match {
          case e: AutoBrand => rdbms.save(e)
        }
      }
    }

    fromFile("sample/seeds").foreach(seed => {
      fireEvent(seed._2)
    })

    shutdown
  }

  def parse(document: Document): List[AutoBrand] = {
    var brands = List[AutoBrand]()
    $(document).filter("dl").foreach(dl => {
      var brand = new AutoBrand
      brand.setFetchedAt(now)
      dl.filter("dt").foreach(dt => {
        dt.filter("a").filter(link => isNotEmpty(link.text())).foreach(link => {
          brand.setName(link.text)
          brand.setUrl(link.attr("href"))
        })
      })

      /* 分析品牌下的车型 */
      dl.filter("li").filter(li => isNotEmpty(li.attr("id"))).foreach(li => {
        var autocar = new AutoCar
        li.filter("h4").foreach(h4 => h4.filter("a").filter(link => isNotEmpty(link.text)).foreach(link => {
          autocar.setName(link.text)
          autocar.setUrl(link.attr("href"))
        }))

        /* 分析价格 */
        li.filter(".red -> a").foreach(link => autocar.setGovPrice(formatPrice(link.text())))

        autocar.setFetchedAt(now)
        brand.getAutos.add(autocar)
      })
      brands = brands.+:(brand)
    })

    brands.filter(brand => isNotEmpty(brand.getName))
  }

  private def formatPrice(s: String): Double = {
    var rt = 0d
    "\\d+\\.\\d+".r.findAllMatchIn(s).foreach { x => rt = x.toString.toDouble }
    rt
  }

  object NilFetch extends Fetchable {
    def getDirectUrl = ""
  }
}