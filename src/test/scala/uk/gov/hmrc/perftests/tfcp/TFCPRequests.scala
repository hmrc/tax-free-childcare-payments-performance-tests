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

package uk.gov.hmrc.perftests.tfcp

import io.gatling.core.Predef._
import io.gatling.core.check.CheckBuilder
import io.gatling.http.Predef._
import io.gatling.http.check.header.HttpHeaderCheckType
import io.gatling.http.request.builder.HttpRequestBuilder
import uk.gov.hmrc.performance.conf.ServicesConfiguration

object TFCPRequests extends ServicesConfiguration {

  val bearerToken: String = readProperty("bearerToken", "${authBearerToken}")

  val baseUrl: String = baseUrlFor("tfcp") + "/individuals/tax-free-childcare/payments"

  lazy val authBaseUrl: String = baseUrlFor("auth-login-api")

  lazy val authUrl: String = s"$authBaseUrl/government-gateway/session/login"

  val postAuthApiSessionLogin: HttpRequestBuilder =
    http("Post to Auth API Session Login")
      .post(authUrl)
      .body(StringBody(authPayload("AB123456A")))
      .header("Content-Type", "application/json")
      .check(saveAuthBearerToken)

  def saveAuthBearerToken: CheckBuilder[HttpHeaderCheckType, Response, String] =
    header(HttpHeaderNames.Authorization).saveAs("authBearerToken")

  val postLink: HttpRequestBuilder =
    http("Payment Link Request")
      .post(s"$baseUrl/link")
      .header("Authorization", s"$bearerToken")
      .header("Content-Type", "application/json")
      .header("Accept", "application/vnd.hmrc.1.0+json")
      .header("Correlation-ID", "5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b")
      .body(StringBody(linkPayload()))
      .asJson
      .check(status.is(200))

  val postBalance: HttpRequestBuilder =
    http("Post link endpoint")
      .post(s"$baseUrl/balance")
      .header("Content-Type", "application/json")
      .header("Accept", "application/vnd.hmrc.1.0+json")
      .header("Authorization", s"$bearerToken")
      .header("Correlation-ID", "5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b")
      .body(StringBody(balancePayload()))
      .check(status.is(200))

  val postPayment: HttpRequestBuilder =
    http("Post link endpoint")
      .post(s"$baseUrl/")
      .header("Content-Type", "application/json")
      .header("Accept", "application/vnd.hmrc.1.0+json")
      .header("Authorization", s"$bearerToken")
      .header("Correlation-ID", "5c5ef9c2-72e8-4d4f-901e-9fec3db8c64b")
      .body(StringBody(paymentPayload()))
      .check(status.is(200))

  def authPayload(nino: String): String =
    s"""
       |{
       |  "credId": "$nino",
       |  "affinityGroup": "Individual",
       |  "confidenceLevel": 250,
       |  "credentialStrength": "strong",
       |  "enrolments": [],
       |  "nino": "$nino"
       |}
       |""".stripMargin
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
