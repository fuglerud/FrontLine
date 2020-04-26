package magnus

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._


class DemoSimulation extends Simulation{

  val httpConf = http
    .baseUrl("http://computer-database.gatling.io")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val scn = scenario("Demoimulation")

    .feed(csv("data/demoSim_Computers.csv").circular)

    .exec(http("Base")
    .get("/")
    .check(status.is(200)))

    .exec(http("Search")
    .get("/computers?f=${maskiner}")
    .check(status.is(200))
    .check(regex("${maskiner}").exists))


  //setUp(scn.inject(constantUsersPerSec(10) during (10 seconds))).protocols(httpConf)
  setUp(scn.inject(atOnceUsers(1))).protocols(httpConf)
}
