package magnus

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class DigitaleKupongerSimulation extends Simulation {

  val httpConf = http
    .acceptEncodingHeader("gzip,deflate")
    .headers(Map("Content-Type" -> "application/json", "charset" -> "UTF-8",  "User-Agent" -> "Android(4.4)/Coop(0.4)/0.1"))

  val scn = scenario("Scenario")

    .feed(csv("data/memberid.csv").random)
    .exec(_.set("itemList", Seq.empty[Int]))

    .exec(http("getProfile")
        .get("/user/profile")
        .header( "X-Token", "${MemberId}" )
        .check(status.is(200))
        .check(jsonPath("$.resultCode").is("SUCCESS"))
        .check(jsonPath("$.profile.memberships[0].number").is("${MemberId}")))

    .exec(http("getCouponList")
      .get("/coupon/all")
      .header("X-Token", "${MemberId}")
      .check(status.is(200))
      .check(jsonPath("$.membershipCoupons[*].coupons[?(@.activationDate==null)].offerId").findAll.optional.saveAs("itemList")))

      doIf(session => ! session("itemList").as[Seq[String]].isEmpty){
      foreach("${itemList}", "item"){
        exec(http("activateCoupon")
          .post("/coupon/activate")
          .header("X-Token", "${MemberId}")
         // .body(StringBody("""{"offerId": "${item}", "membershipId": "${MemberID}", "osName": "Android", "osVersion": "4.4"}""")).asJSON
          .check(jsonPath("$.resultCode").is("SUCCESS")))
      }
    }.exitHereIfFailed

  setUp(scn.inject(atOnceUsers(1))).protocols(httpConf)
  //setUp(scn.inject(constantUsersPerSec(1) during (1 seconds))).protocols(httpConf)
}