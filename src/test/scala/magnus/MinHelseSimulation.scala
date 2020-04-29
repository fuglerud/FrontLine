package magnus

import io.gatling.core.Predef._
import io.gatling.http.Predef._


class MinHelseSimulation extends Simulation{

  val httpConf = http
    .baseUrl("https://minhelse.hn.test.nhn.no")
    .inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.woff2""", """.*\.(t|o)tf""", """.*\.png""", """.*detectportal\.firefox\.com.*"""), WhiteList())
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.9")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.117 Safari/537.36")

  val headers_1 = Map("Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9", "Sec-Fetch-Mode" -> "navigate", "Sec-Fetch-Site" -> "none", "Upgrade-Insecure-Requests" -> "1")
  val headers_2 = Map("Accept-Encoding" -> "gzip, deflate, br", "Accept-Language" -> "en-US,en;q=0.9", "Sec-Fetch-Mode" -> "cors", "Sec-Fetch-Site" -> "same-origin", "accept" -> "application/json", "content-type" -> "application/json", "hnanonymoushash" -> "${AnonymousHash}", "hnauthenticatedhash" -> "${AuthenticatedHash}", "hntimestamp" -> "${TimeStamp}", "hntjeneste" -> "${TjenesteType}", "x-hn-hendelselogg" -> "Min-Helse forside")
  val headers_3 = Map("Content-Type" -> "application/json")

  val scn = scenario("MinHelseSimulation")

    .feed(csv("data/DIFI_testdata.csv").circular)


    //Hent Pasientjournal for en bruker
    .exec(http(requestName = "request_PasientJournal")
      .get(url = "/Min-pasientjournal/?pnr=${pnr}")
      .headers(headers_1)
      .check(regex("\"__AnonymousHash__\": \"(.*?)\"").saveAs("AnonymousHash"))
      .check(regex("\"__AuthenticatedHash__\": \"(.*?)\"").saveAs("AuthenticatedHash"))
      .check(regex("\"__TimeStamp__\": \"(.*?)\"").saveAs("TimeStamp"))
      .check(regex("\"__TjenesteType__\": \"(.*?)\"").saveAs("TjenesteType")))

    /*
    .exec(session=>{
      println("AnonymousHash:")
      println(session("AnonymousHash").as[String])
      session})
      */


    //Hent Representasjonsforhold for en bruker
    .exec(http("request_GetRepresentasjonsforhold")
      .get("/api/v1/MinHelse/GetRepresentasjonsforhold?FetchUnreadMessages=true")
      .headers(headers_2)
      .check(status.is(expected = 200))
      .check(regex("\"ProfilGuid\":\"(.*?)\"").saveAs("ProfilGuid")))

    /*
    .exec(session=>{
      println("ProfilGuid:")
      println(session("ProfilGuid").as[String])
      session})
      */

    //Hent Minhelse
    .exec(http("request_MinHelse")
      .get("https://minhelse.hn.test.nhn.no")
      .headers(headers_2)
      .check(status.is(expected = 200)))




  setUp(scn.inject(constantUsersPerSec(10) during (10))).protocols(httpConf)
  //setUp(scn.inject(atOnceUsers(1))).protocols(httpConf)
}
