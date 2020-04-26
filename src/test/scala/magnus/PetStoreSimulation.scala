import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class PetStoreSimulation extends Simulation {

	val httpProtocol = http

		.inferHtmlResources()
		.acceptEncodingHeader("gzip,deflate")
		.userAgentHeader("Apache-HttpClient/4.1.1 (java 1.5)")

	val headers_0 = Map("header" -> "Accept: application/json")

	val scn = scenario("PetSimulation")

		.feed(csv("data/CorrelationParameters.csv").circular)

		.exec(http("Pet")
			.get("/pet/1")
			.headers(headers_0)
			.check(status.is(200))
			.check(regex("${CorrelationParameter}"))
			.check(jsonPath("$.name").is("${CorrelationParameter}"))
			.check(jsonPath("$.name").exists.saveAs("itemList"))
		)

	/*
	doIf(session => ! session("itemList").as[Seq[String]].isEmpty){
		foreach("${itemList}", "item"){
			exec(http("activateCoupon")
				.post("/coupon/activate")
				.header("X-Token", "${MemberId}")
				.body(StringBody("""{"offerId": "${item}", "membershipId": "${MemberID}", "osName": "Android", "osVersion": "4.4"}""")).asJSON
				.check(jsonPath("$.resultCode").is("SUCCESS")))
		}
	}.exitHereIfFailed
	*/



	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
	//setUp(scn.inject(constantUsersPerSec(3) during (30 seconds))).protocols(httpProtocol)

}