package magnus

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._
import scala.concurrent.duration._

class IdpSimulation extends Simulation{

  val httpConf = http
    .baseURL("https://id.coop.no/ids")
    .acceptEncodingHeader("gzip,deflate")
    .headers(Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8"))
    .authorizationHeader("Basic bW9iaWxlX2NsaWVudDptb2JpbGVfc2VjcmV0")
    .disableUrlEncoding

  val scn = scenario("Scenario")

    .feed(csv("data/memberid.csv").random)

    .exec(http("getToken")
      .post("/connect/token")
      .body(StringBody("username=${MemberId}&password=pass123&scope=legacy&proxy=http%3A%2F%2Flocalhost%3A8888&grant_type=password"))
      //.body(StringBody("username=${mobilnr}&password=pass123&scope=legacy&proxy=http%3A%2F%2Flocalhost%3A8888&grant_type=password"))
      //.body(StringBody("username=${email}&password=pass123&scope=legacy&proxy=http%3A%2F%2Flocalhost%3A8888&grant_type=password"))
      .asFormUrlEncoded
      .check(jsonPath("$.access_token").exists.saveAs("token")))

    .exec(http("verifyToken")
        .get("/connect/accesstokenvalidation?token=${token}")
        .check(status.is(200)))


  setUp(scn.inject(constantUsersPerSec(1) during (1 seconds))).protocols(httpConf)
}
