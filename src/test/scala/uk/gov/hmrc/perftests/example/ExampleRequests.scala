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
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import jodd.lagarto.dom.NodeSelector
import uk.gov.hmrc.performance.conf.ServicesConfiguration

object ExampleRequests extends ServicesConfiguration {

  val baseUrl: String = baseUrlFor("example-frontend")
  val route: String   = "/check-your-vat-flat-rate"

  val navigateToHomePage: HttpRequestBuilder =
    http("Navigate to Home Page")
      .get("http://localhost:9949/auth-login-stub/gg-sign-in")
      .check(status.is(200))
      //.check(css("input[name=csrfToken]", "value").saveAs("csrfToken"))

  val postVatReturnPeriod: HttpRequestBuilder =
    http("Post VAT return Period")
      .post("http://localhost:9949/auth-login-stub/gg-sign-in")
      .formParam("authorityId", "1234")
      .formParam("redirectionUrl", "http://localhost:9949/auth-login-stub/session")
      .formParam("excludeGnapToken", "true")
      .formParam("credentialStrength", "strong")
      .formParam("confidenceLevel", "250")
      .formParam("affinityGroup", "Individual")
      .formParam("enrolments", "[]")
      .formParam("nino", "AB123456C")
      .check(status.is(303))
      .check(bodyString.saveAs("responseBody"))

//  val getSession: HttpRequestBuilder =
//    http("Get auth login stub session information")
//      .get("http://localhost:9949/auth-login-stub/session")
//      .check(status.is(200))
//      .check(saveBearerToken)

  def saveBearerToken: CheckBuilder[CssCheckType, NodeSelector, String] =
    css("[data-session-id=authToken] > code")
      .saveAs("accessToken")

     // .check(header("Location").is("/check-your-vat-flat-rate/turnover").saveAs("turnOverPage"))

  val getTurnoverPage: HttpRequestBuilder =
    http("Get Turnover Page")
      .get("http://localhost:9949/auth-login-stub/session")
      .check(status.is(200))
      .check(saveBearerToken)

  val postLink: HttpRequestBuilder =
    http("Post link endpoint")
      .post("http://localhost:10500/individuals/tax-free-childcare/payments/link")
      .header("Content-Type","application/json")
      .header("Accept","application/vnd.hmrc.1.0+json")
      .header("Authorization","Bearer BXQ3/Treo4kQCZvVcCqKPjTopmv6LMjAMmvpCMEn1P3Lm1K/vpQE6qsfVpjwFl1pS3Mtur8rWkYLKTCGgYl6WTt3OjlofUZ/KAfl1yNIi6wYpN1us94sbQ8TtQmIiWcloSkelzst75MuEbhxFH6ZJNj54dWH7hL34rUOKikcYPn9KwIkeIPK/mMlBESjue4V")
      .header("Correlation-ID","5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b")
      //      .headers(Map("Content-Type" -> "application/json"))
//      .headers(Map("Accept" -> "application/vnd.hmrc.1.0+json"))
//      .headers(Map("Authorization" -> "Bearer BXQ3/Treo4kQCZvVcCqKPtZy+ZKR2wkzNJDCVnBN2BkA4Ixr9RFUDWcw//PoXn3yvXvh3449fu6ujbm7AtiPwZapx2CLx5DvZ2No9zg3NpMA5p47C6pzqau9B9Yd9j42gC2TIr2IR3Dk2OPMMxLbPwiuQZQLltdYel9h2dlePZz9KwIkeIPK/mMlBESjue4V"))
//      .headers(Map("Correlation-ID" -> "5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b"))
      .body(StringBody(linkPayload()))
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
}
