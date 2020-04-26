package magnus

import scala.concurrent.duration._
import java.util.Date
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class NeptuneSimulation extends Simulation{

  val myGlobalVar = new java.util.concurrent.atomic.AtomicInteger(0)

  val httpProtocol = http

    .inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.(t|o)tf""", """.*\.png"""), WhiteList())
    .acceptHeader("application/json, text/javascript, */*; q=0.01")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .connectionHeader("keep-alive")
    .contentTypeHeader("application/x-www-form-urlencoded; charset=UTF-8")
    .userAgentHeader("Mozilla/5.0 (Windows NT 6.3; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0")
  //.userAgentHeader("Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.125 Safari/537.36")

  val headers = Map("Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", "X-Requested-With" -> "XMLHttpRequest", "X-SUP-APPCID" -> "d133dd27-2631-477b-8b9e-896689944e1a", "sap-client" -> "300")


  val scnVareInformasjon = scenario("VareInformasjon")

    .feed(csv("data/pdaArticle.csv").random)
    .feed(csv("data/pdaTestData.csv").random)
    .feed(csv("data/PdaStoreNames.csv").random)
    .feed(csv("data/bestillingsvarerQ30.csv").random)

    .exec(flushCookieJar)
    .exec(flushHttpCache)

    .exec(http("LOGIN")
      .get("/UI5_RETAIL_APPCACHE?sap-client=300&appcache=COOP_Q30_UTEN_PINKODE")
      .headers(headers)
      .basicAuth("${username}","${password}")
      .check(status.is(200)))

    .exec(http("GET_STORE")
      .post("/native/neptune_ajax?ajax_id=GET_STORE&ajax_applid=UI5_RETAIL_ARTICLE_INFO&field_id=00090&ajax_value=undefined")
      .headers(headers)
      .check(status.is(200)))

    .exec(http("GET_ARTICLE_DETAILS")
      .post("/neptune/native/neptune_ajax?ajax_id=GET_ARTICLE_DETAILS&ajax_applid=UI5_RETAIL_ARTICLE_INFO&field_id=00075&ajax_value=${art}")
      .formParam("""{"WERKS":"${storenumber}","NAME1":"${storename}","MATNR":"","BISMT":"","EAN11":"","MAKTX":"","MEINS":"","BSTME":""}||{"WERKS":"${storenumber}","NAME1":"${storename}":"","MATNR":"","MATNR_TIMEOUT":"","TILE_TITLE":"Vareinformasjon","TRANSACTION_TYPE":"","WERKS_TIMEOUT":"","FORCE_OFFLINE":false,"DISABLE_HAPTIC":false,"REASON_TIMEOUT":""}""", "")
      .headers(headers)
      .check(status.is(200))
      .check(jsonPath("$.modeltabArticleData[?(@.DETAIL_TYPE=='EAN')].DETAIL_VALUE").is("${art}"))
      .check(jsonPath("$.modeltabArticleData[?(@.DETAIL_TYPE=='ORIGINAL_PRICE')].DETAIL_VALUE").exists))

    .exec(http("LOGOUT")
      .post("https://sap_pda_ecc_q30.coop.no/neptune/native/neptune_login_ping.html?sap-clearsso2")
      .check(regex("""<body ><div id=\"ping\"></div>""")))


  val scnVarebestilling = scenario("Varebestilling")

    .feed(csv("data/pdaTestData.csv").random)
    .feed(csv("data/PdaStoreNames.csv").random)
    .feed(csv("data/bestillingsvarerQ30.csv").random)

    .exec(flushCookieJar)
    .exec(flushHttpCache)

    .exec(http("LOGIN")
      .get("/UI5_RETAIL_APPCACHE?sap-client=300&appcache=COOP_Q30_UTEN_PINKODE")
      .headers(headers)
      .basicAuth("${username}","${password}")
      .check(status.is(200)))

    .exec(session => session.set("timestamp", (new Date().getTime + 1 + myGlobalVar.getAndIncrement).toString))

    .exec(http("GET_CARTS_TOTAL")
      .post("/native/neptune_ajax?ajax_id=GET_CARTS_TOTAL&ajax_applid=UI5_RETAIL_CREATE_PO&field_id=00309&ajax_value=undefined")
      .headers(headers)
      .formParam("""{"WERKS":"${storenumber}","NAME1":"${storename}","LIST_ID":${timestamp},"DEVICE_ID":"${timestamp}"}""", "")
      .check(status.is(200))
      .check(jsonPath("$.modelpageStartData.TOTAL_LISTS").saveAs("total_lists")))

    .repeat(10)
    {
      exec(http("GET_ARTICLE_DETAILS")
        .post("/native/neptune_ajax?ajax_id=GET_ARTICLE_DETAILS&ajax_applid=UI5_RETAIL_CREATE_PO&field_id=00055&ajax_value=${vare}")
        .formParam("""{"WERKS":"${storenumber}","NAME1":"${storename}","LIST_ID":"${timestamp}","TOTAL_LISTS":${total_lists},"DEVICE_ID":"${timestamp}"}||{}||{}||{}""", "")
        .headers(headers)
        .check(jsonPath("$.modelsfrmArticleData.MATNR").saveAs("matnr"))
        .check(jsonPath("$.modelsfrmArticleData.MAKTX").saveAs("maktx"))
        .check(jsonPath("$.modelsfrmArticleData.CHANGED_DATE").saveAs("changed_date"))
        .check(jsonPath("$.modelsfrmArticleData.CHANGED_TIME").saveAs("changed_time"))
        .check(jsonPath("$.modelsfrmArticleData.VAR_PO_UNIT_POSSIBLE").saveAs("var_po_unit_possible"))
        .check(jsonPath("$.modelsfrmArticleData.VAR_PO_UNIT").saveAs("var_po_unit"))
        .check(jsonPath("$.modelsfrmArticleData.DELETE_RECORD").saveAs("delete_record"))
        .check(jsonPath("$.modelsfrmArticleData.WARNING").saveAs("warning"))
        .check(jsonPath("$.modelsfrmArticleData.BSTME").saveAs("bstme"))
        .check(jsonPath("$.modelsfrmArticleData.MEINS").saveAs("meins"))
        .check(jsonPath("$.modelsfrmArticleData.FP_IN_DP").saveAs("fp_in_dp"))
        .check(regex("""Artikkel lagt til""")))
    }

    .exec(http("SEND_CART")
      .post("/neptune/native/neptune_ajax?ajax_id=SEND_CART&ajax_applid=UI5_RETAIL_CREATE_PO&field_id=00232&ajax_value=undefined")
      .headers(headers)
      .formParam("""{"WERKS":"${storenumber}","NAME1":"${storename}","LIST_ID":"${timestamp}","TOTAL_LISTS":${total_lists},"DEVICE_ID":"${timestamp}"}||[{"MATNR":"${matnr}","EAN11":"","MAKTX":"${maktx}","QUANTITY":1,"MEINS":"${meins}","BSTME":"${bstme}","FP_IN_DP":"${fp_in_dp} ","CHANGED_DATE":"${changed_date}","CHANGED_TIME":"${changed_time}","CHANGED_UNAME":"${username}","CHANGED_DEVICE":"${timestamp}","VAR_PO_UNIT_POSSIBLE":${var_po_unit_possible},"VAR_PO_UNIT":false,"ZZPALLVAR":"","SYNC_STATE":"","DELETE_RECORD":${delete_record},"WARNING":${warning}}]||[]||{}""", "")
      .check(regex("""Ordren ble opprettet""")))

    .exec(http("LOGOUT")
      .post("https://sap_pda_ecc_q30.coop.no/neptune/native/neptune_login_ping.html?sap-clearsso2")
      .check(regex("""<body ><div id=\"ping\"></div>""")))
















  val scnEtikettbestilling = scenario ("Etikettbestilling")

    .feed(csv("data/pdaArticle.csv").random)
    .feed(csv("data/pdaTestData.csv").random)
    .feed(csv("data/PdaStoreNames.csv").random)
    .feed(csv("data/bestillingsvarerQ30.csv").random)

    .exec(flushCookieJar)
    .exec(flushHttpCache)

    //.exec(_.set("timestamp2", new Date().getTime)) // sets "timestamp2" to millis since epoch.
    .exec(session => session.set("timestamp2", (new Date().getTime + 2 + myGlobalVar.getAndIncrement).toString))

    .exec(http("LOGIN")
      .get("/UI5_RETAIL_APPCACHE?sap-client=300&appcache=COOP_Q30_UTEN_PINKODE")
      .headers(headers)
      .basicAuth("${username}","${password}")
      .check(status.is(200)))

    .exec(http("GET_CARTS_TOTAL")
      .post("/neptune/native/neptune_ajax?ajax_id=GET_CARTS_TOTAL&ajax_applid=UI5_RETAIL_ORDER_LABEL&field_id=00309&ajax_value=undefined")
      .formParam("""{"WERKS":"${storenumber}","NAME1":"${storename}","LIST_ID":${timestamp2},"DEVICE_ID":"${timestamp2}"}""", "")
      .check(jsonPath("$.modelpageStartData.TOTAL_LISTS").saveAs("total_lists"))
      .check(jsonPath("$.modelpageStartData.ID").saveAs("id")))

    .exec(http("LOGOUT")
      .post("https://sap_pda_ecc_q30.coop.no/neptune/native/neptune_login_ping.html?sap-clearsso2")
      .check(regex("""<body ><div id=\"ping\"></div>""")))



  val scnVaretelling = scenario ("Varetelling")

    .feed(csv("data/pdaArticle.csv").circular)
    .feed(csv("data/pdaTestData.csv").random)
    .feed(csv("data/PdaStoreNames.csv").random)
    .feed(csv("data/bestillingsvarerQ30.csv").random)

    .exec(flushCookieJar)
    .exec(flushHttpCache)

    .exec(session => session.set("timestamp3", (new Date().getTime + 333 + myGlobalVar.getAndIncrement).toString))

    .exec(http("Inventory_LOGIN")
      .get("/UI5_RETAIL_APPCACHE?sap-client=300&appcache=COOP_Q30_UTEN_PINKODE")
      .headers(headers)
      .basicAuth("${username}","${password}")
      .check(status.is(200)))

    .exec(http("inventory_GET_CARTS_TOTAL")
      .post("/native/neptune_ajax?ajax_id=GET_CARTS_TOTAL&ajax_applid=UI5_RETAIL_TRANSACTIONS&field_id=00309&ajax_value=undefined")
      .formParam("""{"WERKS":"${storenumber}","NAME1":"COOP OBS! BYGG TROMSØ, COOP NO","LIST_ID":0,"TOTAL_LISTS":0,"DEVICE_ID":"${timestamp3}","HEADER_TEXT":"","HASH_BASED_MESSAGE_AUTHENTICAT":"","ID":"","REVISION":0,"CREATE_NEW_CART":false}||{"WERKS":"${storenumber}","NAME1":"COOP OBS! BYGG TROMSØ, COOP NO","LIST_ID":"","MATNR":"","MATNR_TIMEOUT":"","TILE_TITLE":"Varetelling","TRANSACTION_TYPE":"Inventory","WERKS_TIMEOUT":"${timestamp3}","FORCE_OFFLINE":false,"DISABLE_HAPTIC":false,"REASON_TIMEOUT":"${timestamp3}","UNAME":"${username}"}""", "")
      .headers(headers)
      .check(status.is(200))
      .check(jsonPath("$.modelpageStartData.TOTAL_LISTS").saveAs("total_lists")))

    .exec(http("Inventory_GET_ARTICLE_DETAILS")
      .post("https://sap_pda_ecc_q30.coop.no/neptune/native/neptune_ajax?ajax_id=GET_ARTICLE_DETAILS&ajax_applid=UI5_RETAIL_TRANSACTIONS&field_id=00055&ajax_value=${art}")
      .formParam("""{"WERKS":"${storenumber}","NAME1":"COOP OBS! BYGG TROMSØ, COOP NO","LIST_ID":${timestamp3},"TOTAL_LISTS":"${total_lists}","DEVICE_ID":"${timestamp3}","HEADER_TEXT":"","HASH_BASED_MESSAGE_AUTHENTICAT":"","ID":"","REVISION":0,"CREATE_NEW_CART":false}||{}||{}||{"WERKS":"${storenumber}","NAME1":"COOP OBS! BYGG TROMSØ, COOP NO","LIST_ID":${timestamp3},"MATNR":"","MATNR_TIMEOUT":"","TILE_TITLE":"Varetelling","TRANSACTION_TYPE":"Inventory","WERKS_TIMEOUT":${timestamp3},"FORCE_OFFLINE":false,"DISABLE_HAPTIC":false,"REASON_TIMEOUT":${timestamp3},"UNAME":"${username}"}""", "")
      .headers(headers)
      .check(jsonPath("$.modelsfrmArticleData.MATNR").saveAs("matnr"))
      .check(jsonPath("$.modelsfrmArticleData.MAKTX").saveAs("maktx"))
      .check(jsonPath("$.modelsfrmArticleData.CHANGED_DATE").saveAs("changed_date"))
      .check(jsonPath("$.modelsfrmArticleData.CHANGED_TIME").saveAs("changed_time"))
      .check(jsonPath("$.modelsfrmArticleData.EAN11").saveAs("ean11"))
      .check(jsonPath("$.modelsfrmArticleData.NORMAL_PRICE").saveAs("normal_price"))
      .check(jsonPath("$.modelpageStartData.ID").saveAs("lindbak_id")))

    .exec(http("SYNC_OUTBOX_ASYNCHRONOUS")
      .post("https://sap_pda_ecc_q30.coop.no/neptune/native/neptune_ajax?ajax_id=SYNC_OUTBOX_ASYNCHRONOUS&ajax_applid=UI5_RETAIL_TRANSACTIONS&field_id=00317&ajax_value=undefined")
      .headers(headers)
      .formParam("""{"WERKS":"2371","NAME1":"COOP OBS! BYGG TROMSØ, COOP NO","LIST_ID":"${timestamp3}","TOTAL_LISTS":${total_lists},"DEVICE_ID":"${timestamp3}","HEADER_TEXT":"","HASH_BASED_MESSAGE_AUTHENTICAT":"42","ID":"${lindbak_id}","REVISION":0,"CREATE_NEW_CART":false}||[{"MATNR":"${matnr}","EAN11":"${ean11}","MAKTX":"${maktx}","QUANTITY":1,"MEINS":"","BSTME":"","FP_IN_DP":"","CHANGED_DATE":"${changed_date}","CHANGED_TIME":"${changed_time}","CHANGED_UNAME":"${username}","CHANGED_DEVICE":"${timestamp3}","DELETE_RECORD":false,"AUGRU":0,"AUGRU_DESC":"","MATNR_KEY":"Inventory_0","CART_ITEM_IDENTIFICATOR":0,"NORMAL_PRICE":"${normal_price}","TOTAL":"0.0000000","IS_PARENT":"","PARENT_CART_ITEM_IDENTIFICATOR":"","IS_CHILD":"","LIST_SUBTYPE":"Inventory","UNIT_OF_MEASURE":"STK"}]||{"WERKS":"2371","NAME1":"COOP OBS! BYGG TROMSØ, COOP NO","LIST_ID":"${timestamp3}","MATNR":"","MATNR_TIMEOUT":"","TILE_TITLE":"Varetelling","TRANSACTION_TYPE":"Inventory","WERKS_TIMEOUT":"${timestamp3}","FORCE_OFFLINE":false,"DISABLE_HAPTIC":false,"REASON_TIMEOUT":"${timestamp3}","UNAME":"${username}"}||[]||[{"MATNR":"${matnr}","EAN11":"${ean11}","MAKTX":"${maktx}","QUANTITY":1,"MEINS":"","BSTME":"","FP_IN_DP":"","CHANGED_DATE":"${changed_date}","CHANGED_TIME":"${changed_time}","CHANGED_UNAME":"${username}","CHANGED_DEVICE":"${timestamp3}","DELETE_RECORD":false,"AUGRU":0,"AUGRU_DESC":"","MATNR_KEY":"Inventory_0","CART_ITEM_IDENTIFICATOR":0,"NORMAL_PRICE":"${normal_price}","TOTAL":"0.0000000","IS_PARENT":"","PARENT_CART_ITEM_IDENTIFICATOR":"","IS_CHILD":"","LIST_SUBTYPE":"Inventory","UNIT_OF_MEASURE":"STK"}]:""",""))

    .exec(http("Inventory_SEND_CART")
      .post("https://sap_pda_ecc_q30.coop.no/neptune/native/neptune_ajax?ajax_id=SEND_CART&ajax_applid=UI5_RETAIL_TRANSACTIONS&field_id=00232&ajax_value=undefined")
      .formParam("""{"WERKS":"${storenumber}","NAME1":"COOP OBS! BYGG TROMSØ, COOP NO","LIST_ID":"${timestamp3}","TOTAL_LISTS":${total_lists},"DEVICE_ID":"${timestamp3}","HEADER_TEXT":"","HASH_BASED_MESSAGE_AUTHENTICAT":"42","ID":"${lindbak_id}","REVISION":0,"CREATE_NEW_CART":false}||[{"MATNR":"${matnr}","EAN11":"${ean11}","MAKTX":"${maktx}","QUANTITY":1,"MEINS":"","BSTME":"","FP_IN_DP":"","CHANGED_DATE":"${changed_date}","CHANGED_TIME":"${changed_time}","CHANGED_UNAME":"${username}","CHANGED_DEVICE":"${timestamp3}","DELETE_RECORD":false,"AUGRU":0,"AUGRU_DESC":"","MATNR_KEY":"Inventory_0","CART_ITEM_IDENTIFICATOR":0,"NORMAL_PRICE":"${normal_price}","TOTAL":"0.0000000","IS_PARENT":"","PARENT_CART_ITEM_IDENTIFICATOR":"","IS_CHILD":"","LIST_SUBTYPE":"Inventory","UNIT_OF_MEASURE":"STK"}]||{"WERKS":"${storenumber}","NAME1":"COOP OBS! BYGG TROMSØ, COOP NO","LIST_ID":"${timestamp3}","MATNR":"","MATNR_TIMEOUT":"","TILE_TITLE":"Varetelling","TRANSACTION_TYPE":"Inventory","WERKS_TIMEOUT":"${timestamp3}","FORCE_OFFLINE":false,"DISABLE_HAPTIC":false,"REASON_TIMEOUT":"${timestamp3}","UNAME":"${username}"}""", "")
      .headers(headers)
      .check(regex("""Handlekurv ble sendt""")))

    .exec(http("Inventory_LOGOUT")
      .post("https://sap_pda_ecc_q30.coop.no/neptune/native/neptune_login_ping.html?sap-clearsso2")
      .check(regex("""<body ><div id=\"ping\"></div>""")))



  //setUp(scnVareInformasjon.inject(atOnceUsers(1))).protocols(httpProtocol)
  //setUp(scnVarebestilling.inject(atOnceUsers(5))).protocols(httpProtocol)
  setUp(scnEtikettbestilling.inject(atOnceUsers(1))).protocols(httpProtocol)
  //setUp(scnVaretelling.inject(atOnceUsers(1))).protocols(httpProtocol)
  //setUp(scnVarebestilling.inject(constantUsersPerSec(10) during (20 seconds))).protocols(httpProtocol)
  //setUp(scnVareInformasjon.inject(constantUsersPerSec(10) during (20 seconds))).protocols(httpProtocol)
  //setUp(scnVaretelling.inject(constantUsersPerSec(10) during (20 seconds))).protocols(httpProtocol)

}
