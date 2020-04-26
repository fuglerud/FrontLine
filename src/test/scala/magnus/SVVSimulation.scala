import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class SVVSimulation extends Simulation {

  val httpProtocol = http
      .baseURL("http://svvutomcat19:17450")
    //.baseUrl("https://www.test.vegvesen.no")
    //.baseUrl("https://www.utv.vegvesen.no")
    .inferHtmlResources()
    .acceptEncodingHeader("gzip,deflate")
    .userAgentHeader("Apache-HttpClient/4.1.1 (java 1.5)")

  val headers = Map("Cookie2" -> "$Version=1")


  //Scenario for full JSON
  val scnJson = scenario("JSON")

    .feed(csv("data/TestdataDatexJSON.csv").circular)

    .exec(http("request_MongoDB")
      .get("/trafikkna/geoserver/datex_3_0/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=datex_3_0%3ASituation&outputFormat=application%2Fjson&filter=<Filter><PropertyIsEqualTo><PropertyName>Situation/situationRecord/roadOrCarriagewayOrLaneManagementType</PropertyName><Literal>${Literal}</Literal></PropertyIsEqualTo></Filter>")
      .headers(headers)
      //.body(ElFileBody("DataGeoserver_request.xml"))
      .check(status.is(200)))

  //Scenario for BBOX
  val scnBbox = scenario("BBOX")

    .feed(csv("data/TestdataDatexBBOX.csv").circular)

    .exec(http("request_Bbox")
      .get("/trafikkna/geoserver/wms/?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=datex_3_0%3AForecastCondition&TILED=true&filter=%3CFilter%3E%3CPropertyIsEqualTo%3E%3CPropertyName%3EphysicalQuantity%2FbasicData%2FhourOfDay%3C%2FPropertyName%3E%3CLiteral%3E14%3C%2FLiteral%3E%3C%2FPropertyIsEqualTo%3E%3C%2FFilter%3E&WIDTH=256&HEIGHT=256&SRS=EPSG%3A32633&STYLES=&BBOX=${BBOX}")
      .headers(headers)
      .check(status.is(200)))

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