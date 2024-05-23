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

import scala.util.Random

object TFCPRequests extends ServicesConfiguration {

  val bearerToken: String = readProperty("bearerToken", "${authBearerToken}")

  val baseUrl: String = baseUrlFor("tfcp") + "/individuals/tax-free-childcare/payments"

  lazy val authBaseUrl: String = baseUrlFor("auth-login-api")

  lazy val authUrl: String = s"$authBaseUrl/government-gateway/session/login"

  val postAuthApiSessionLogin: HttpRequestBuilder =
    http("Post to Auth API Session Login")
      .post(authUrl)
      .body(StringBody(authPayload("${nino}")))
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
      .header("Correlation-ID", "${corelationId}")
      .body(StringBody(linkPayload("${eppUniqueCustomerId}","${eppRegReference}","${outboundChildPaymentRef}","${childDateOfBirth}")))
      .asJson
      .check(status.is(200))

  val postBalance: HttpRequestBuilder =
    http("Post balance endpoint")
      .post(s"$baseUrl/balance")
      .header("Content-Type", "application/json")
      .header("Accept", "application/vnd.hmrc.1.0+json")
      .header("Authorization", s"$bearerToken")
      .header("Correlation-ID", "${corelationId}")
      .body(StringBody(balancePayload("${eppUniqueCustomerId}","${eppRegReference}","${outboundChildPaymentRef}")))
      .check(status.is(200))

  val postPayment: HttpRequestBuilder =
    http("Post payment endpoint")
      .post(s"$baseUrl/")
      .header("Content-Type", "application/json")
      .header("Accept", "application/vnd.hmrc.1.0+json")
      .header("Authorization", s"$bearerToken")
      .header("Correlation-ID", "${corelationId}")
      .body(StringBody(paymentPayload("${eppUniqueCustomerId}","${eppRegReference}","${outboundChildPaymentRef}","${ccpRegReference}","${ccpPostcode}","${payeeType}","${paymentAmount}")))
      .check(status.is(200))


  def authPayload(nino: String): String =
    s"""
       |{
       |  "credId": "$credID",
       |  "affinityGroup": "Individual",
       |  "confidenceLevel": 250,
       |  "credentialStrength": "strong",
       |  "enrolments": [],
       |  "nino": "$nino"
       |}
       |""".stripMargin

  def credID:String =
    Array.fill(16)(Random.nextInt(10)).mkString

  def linkPayload(eppUniqueCustomerId:String,eppRegReference:String,outboundChildPaymentRef:String,childDateOfBirth:String
  ): String =
    s"""
       | {
       | "epp_unique_customer_id":"$eppUniqueCustomerId",
       | "epp_reg_reference":"$eppRegReference",
       | "outbound_child_payment_ref":"$outboundChildPaymentRef",
       | "child_date_of_birth":"$childDateOfBirth"
       | }
    """.stripMargin

  def balancePayload(eppUniqueCustomerId:String,eppRegReference:String,outboundChildPaymentRef:String
  ): String =
    s"""
       | {
       | "epp_unique_customer_id":"$eppUniqueCustomerId",
       | "epp_reg_reference":"$eppRegReference",
       | "outbound_child_payment_ref":"$outboundChildPaymentRef"
       | }
    """.stripMargin

  def paymentPayload(eppUniqueCustomerId:String,eppRegReference:String,outboundChildPaymentRef:String,ccpRegReference:String,ccpPostcode:String,payeeType:String,paymentAmount:String
  ): String =
    s"""
       | {
       | "epp_unique_customer_id":"$eppUniqueCustomerId",
       | "epp_reg_reference":"$eppRegReference",
       | "payment_amount":"$paymentAmount",
       | "ccp_reg_reference": "$ccpRegReference",
       | "ccp_postcode": "$ccpPostcode",
       | "payee_type": "$payeeType",
       | "outbound_child_payment_ref": "$outboundChildPaymentRef"
       | }
    """.stripMargin
}
