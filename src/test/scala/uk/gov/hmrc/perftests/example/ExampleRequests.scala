/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.perftests.example

import io.gatling.core.Predef._
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.css.CssCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import jodd.lagarto.dom.NodeSelector
import uk.gov.hmrc.performance.conf.ServicesConfiguration

object ExampleRequests extends ServicesConfiguration {

  val bearerToken: String   = readProperty("bearerToken", "${accessToken}")

  val baseUrl: String       = baseUrlFor("tfcp")+"/individuals/tax-free-childcare/payments"
  lazy val CsrfPattern                  = """<input type="hidden" name="csrfToken" value="([^"]+)""""
  lazy val baseUrl_Auth: String         = baseUrlFor("auth-login-stub")

   lazy val authBaseUrl: String  = baseUrlFor("auth-login-stub")

  lazy val authUrl: String = s"$authBaseUrl/auth-login-stub/gg-sign-in"
  lazy val redirectionUrl  = s"$authBaseUrl/auth-login-stub/session"


  def saveCsrfToken(): CheckBuilder[RegexCheckType, String, String] = regex(_ => CsrfPattern).saveAs("csrfToken")



  def postAuthLogin(): HttpRequestBuilder =
    http("Enter Auth login credentials ")
      .post(authUrl)
  .formParam("authorityId", "1234")
    .formParam("redirectionUrl", redirectionUrl)
    .formParam("excludeGnapToken", "true")
    .formParam("credentialStrength", "strong")
    .formParam("confidenceLevel", "250")
    .formParam("affinityGroup", "Individual")
    .formParam("enrolments", "[]")
    .formParam("nino", "AB123456C")
      .check(status.is(303))
      .check(bodyString.saveAs("responseBody"))

  val getSession: HttpRequestBuilder =
    http("Get auth login stub session information")
      .get(redirectionUrl)
      .check(status.is(200))
      .check(saveBearerToken)

  def saveBearerToken: CheckBuilder[CssCheckType, NodeSelector, String] =
    css("[data-session-id=authToken] > code")
      .saveAs("accessToken")

//  val postLink: HttpRequestBuilder =
//    http("Post link endpoint")
//      .post(s"$baseUrl/link")
//      .header("Content-Type","application/json")
//      .header("Accept","application/vnd.hmrc.1.0+json")
//    .header("Authorization",s"$abcbearerToken")
//   //.header("Authorization","Bearer BXQ3/Treo4kQCZvVcCqKPs6CmGhGZHLclRqnlnlCXMr0aNVC3/4v3GVLbudsFY7BMnRmKOGYQQqEKfy/3ToA9aYmUIvE/6DxkhKtZ+zLLUJlYfhU3fGbnBW4xgxOK2wORjCU+2OHTUz6SX0CRLH+xKf6ZEnRizR3FRZWMCy9PbT9KwIkeIPK/mMlBESjue4V")
//    .header("Correlation-ID","5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b")
//      .body(StringBody(linkPayload()))
//      .check(status.is(200))
val sendPaymentRequest : HttpRequestBuilder=
    http("Payment Link Request")
//      .post(s"$baseUrl/link")
//      .header("Content-Type","application/json")
//      .header("Authorization", s"$bearerToken")
//      .header("Accept", "application/vnd.hmrc.1.0+json")
//      .header("Correlation-ID", "5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b")
//      .body(StringBody(linkPayload))
//      .check(status.is(200))
//      .check(jsonPath("$.data").saveAs("responseData"))
.post(s"$baseUrl/link")
      .header("Authorization", s"$bearerToken")
      .header("Content-Type","application/json")
      .header("Accept", "application/vnd.hmrc.1.0+json")
      .header("Correlation-ID", "5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b")
      .body(StringBody(linkPayload())).asJson
      .check(status.not(403), status.not(404)) // Check that the status is not 403 or 404
      .check(status.is(200)) // Check for a successful response
      .check(jsonPath("$.data").saveAs("responseData"))
//  .exec { session =>
//    println("Response Data: " + session("responseData").asOption[String].getOrElse("No data found"))
//    session
//  }
  val postBalance: HttpRequestBuilder =
    http("Post link endpoint")
      .post(s"$baseUrl/balance")
      .header("Content-Type","application/json")
      .header("Accept","application/vnd.hmrc.1.0+json")
   //   .header("Authorization",s"$bearerToken")
   .header("Authorization","Bearer BXQ3/Treo4kQCZvVcCqKPs6CmGhGZHLclRqnlnlCXMr0aNVC3/4v3GVLbudsFY7BMnRmKOGYQQqEKfy/3ToA9aYmUIvE/6DxkhKtZ+zLLUJlYfhU3fGbnBW4xgxOK2wORjCU+2OHTUz6SX0CRLH+xKf6ZEnRizR3FRZWMCy9PbT9KwIkeIPK/mMlBESjue4V")
      .header("Correlation-ID","5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b")
      .body(StringBody(balancePayload()))
      .check(status.is(200))

  val postPayment: HttpRequestBuilder =
    http("Post link endpoint")
      .post(s"$baseUrl/")
      .header("Content-Type","application/json")
      .header("Accept","application/vnd.hmrc.1.0+json")
      .header("Authorization",s"$bearerToken")
     //.header("Authorization","Bearer BXQ3/Treo4kQCZvVcCqKPs6CmGhGZHLclRqnlnlCXMr0aNVC3/4v3GVLbudsFY7BMnRmKOGYQQqEKfy/3ToA9aYmUIvE/6DxkhKtZ+zLLUJlYfhU3fGbnBW4xgxOK2wORjCU+2OHTUz6SX0CRLH+xKf6ZEnRizR3FRZWMCy9PbT9KwIkeIPK/mMlBESjue4V")
      .header("Correlation-ID","5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b")
      .body(StringBody(paymentPayload()))
      .check(status.is(200))


  def linkPayload(
                 ): String =
    s"""
       | {
       | "epp_unique_customer_id":"12345678910",
       | "epp_reg_reference":"EPPRegReffEPPReg",
       | "outbound_child_payment_ref":"AAAA00000TFC",
       | "child_date_of_birth":"2023-05-06"
       | }
    """.stripMargin

  def balancePayload(
                 ): String =
    s"""
       | {
       | "epp_unique_customer_id":"12345678910",
       | "epp_reg_reference":"EPPRegReffEPPReg",
       | "outbound_child_payment_ref":"AAAA00000TFC"
       | }
    """.stripMargin

  def paymentPayload(
                    ): String =
    s"""
       | {
       | "epp_unique_customer_id":"12345678910",
       | "epp_reg_reference":"EPPRegReffEPPReg",
       | "payment_amount":1234.56,
       | "ccp_reg_reference": "string",
       | "ccp_postcode": "AB12 3CD",
       | "payee_type": "ccp",
       | "outbound_child_payment_ref": "AAAA00000TFC"
       | }
    """.stripMargin
}
