
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.check.HttpCheck._


class BagasjeSimulation extends Simulation {

  val headers = Map("Content-Type" -> "application/json");

  val httpProtocol = http

    //.baseUrl("https://www.test.vegvesen.no")
    //.baseUrl("https://www.utv.vegvesen.no")
    .inferHtmlResources()
    .acceptEncodingHeader("gzip,deflate")
    .headers(Map("Content-Type" -> "application/json"))


  //Scenario for full JSON
  val scnJson = scenario("JSON")

    .exec(flushCookieJar)
    .exec(flushHttpCache)


    //Kalle token tjenesten
    .exec(http( "request_getToken" )
      .post("/ws/no/vegvesen/ikt/sikkerhet/aaa/autentiser")
      .body(ElFileBody("getToken_request.json"))
      .check(status.is(200))
      .check(jsonPath("$.token").exists.saveAs("token")))

    //Skrive ut token til console
    .exec(session=>{
      println("Token:")
      println(session("token").as[String])
      session})

    //Validere token
    .exec(http( "request_validateToken" )
      .post("/ws/no/vegvesen/ikt/sikkerhet/aaa/validate")
      .body(ElFileBody("validateToken_request.json"))
      .check(status.is(200))
      .check(jsonPath("$.valid").is("true")))



  //  .feed(csv("data/TestdataDatexJSON.csv").circular)

    .exec(http("request_MongoDB")
      .get("/trafikkna/geoserver/datex_3_0/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=datex_3_0%3ASituation&outputFormat=application%2Fjson&filter=<Filter><PropertyIsEqualTo><PropertyName>Situation/situationRecord/roadOrCarriagewayOrLaneManagementType</PropertyName><Literal>${Literal}</Literal></PropertyIsEqualTo></Filter>")
      .headers(Map("tokenname" -> "${token}"))
      .check(status.is(200)))





  //Scenario for BBOX
  val scnBbox = scenario("BBOX")

    .feed(csv("data/TestdataDatexBBOX.csv").circular)

  .exec(http( "request_Bbox" )
     .get("/trafikkna/geoserver/wms/?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=datex_3_0%3AForecastCondition&TILED=true&filter=%3CFilter%3E%3CPropertyIsEqualTo%3E%3CPropertyName%3EphysicalQuantity%2FbasicData%2FhourOfDay%3C%2FPropertyName%3E%3CLiteral%3E14%3C%2FLiteral%3E%3C%2FPropertyIsEqualTo%3E%3C%2FFilter%3E&WIDTH=256&HEIGHT=256&SRS=EPSG%3A32633&STYLES=&BBOX=${BBOX}")
     .headers(headers)
     .check(status.is(200))
     .check(header("Content-Disposition").saveAs("Content-Disposition")))

    .exec(session => {
    println("Response header:")
    println(session( "Content-Disposition" ).as[String])
    session
  })


    //scenario for Tiles
  val scnTiles = scenario("Tiles")

    .feed(csv("data/TestdataDatexTILES.csv").circular)

    .exec(http("request_Tiles")
     .get("/trafikkna/geowebcache/service/wmts/?layer=nvdb&tilematrixset=EPSG%3A32633&Service=WMTS&Request=GetTile&Version=1.0.0&Format=image%2Fjpeg&TileMatrix=EPSG%3A32633%3A6&TileCol=${TileCol}&TileRow=${TileRow}")
     .headers(headers)
     .check(status.is(200)))





  setUp(scnBbox.inject(constantUsersPerSec(5) during(10 seconds)), scnTiles.inject(constantUsersPerSec(4) during(10)), scnJson.inject(atOnceUsers(5))).protocols(httpProtocol)
  //setUp(scnJson.inject(atOnceUsers(1))).protocols(httpProtocol)
  //setUp(scnJson.inject(constantUsersPerSec(5) during (30 seconds))).protocols(httpProtocol)
}
