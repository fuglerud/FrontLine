package magnus

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class CheckEVCSimulation extends Simulation {

	val httpProtocol = http
		.inferHtmlResources()
		.acceptEncodingHeader("gzip,deflate")
		.contentTypeHeader("text/xml;charset=UTF-8")
		.userAgentHeader("Apache-HttpClient/4.1.1 (java 1.5)")

	val headers_check = Map("SOAPAction" -> "http://posapi.cardservices.payex.com/CheckEVC2")

	val scn = scenario("CheckEVCSimulation")

		.exec(http("checkEVC")
			.post("/PxsCardservicesPosApi/ValueCodeService.asmx")
			.headers(headers_check)
			.body(ElFileBody("CheckEVCSimulation_0000_request.txt")))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}