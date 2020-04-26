
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.check.HttpCheck._


class PingNHNSimulation extends Simulation {

  val headers = Map("Content-Type" -> "application/json");

  val httpProtocol = http

    .inferHtmlResources()
    .acceptEncodingHeader("gzip,deflate")
    .headers(Map("Content-Type" -> "application/json"))


  val scnPingNHN = scenario("ping")

    .exec(flushCookieJar)
    .exec(flushHttpCache)

    .exec(http( "request_ping" )
      .post("/api/Ping")
      .check(status.is(200))
    )

  //setUp(scnBbox.inject(constantUsersPerSec(5) during(10 seconds)), scnTiles.inject(constantUsersPerSec(4) during(10)), scnJson.inject(atOnceUsers(5))).protocols(httpProtocol)
  setUp(scnPingNHN.inject(atOnceUsers(1))).protocols(httpProtocol)
  //setUp(scnJson.inject(constantUsersPerSec(5) during (30 seconds))).protocols(httpProtocol)
}
