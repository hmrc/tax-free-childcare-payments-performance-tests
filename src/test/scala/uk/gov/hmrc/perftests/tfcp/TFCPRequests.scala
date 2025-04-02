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
import io.gatling.core.check.css.CssCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.http.Predef._
import io.gatling.http.check.header.HttpHeaderCheckType
import io.gatling.http.request.builder.HttpRequestBuilder
import jodd.lagarto.dom.NodeSelector
import uk.gov.hmrc.performance.conf.ServicesConfiguration

import scala.util.Random
import scala.util.matching.UnanchoredRegex

object TFCPRequests extends ServicesConfiguration {

  val bearerToken: String = readProperty("bearerToken", "${accessToken}")

val baseUrl: String = baseUrlFor("tfcp")

  lazy val CsrfPattern                  = """<input type="hidden" name="csrfToken" value="([^"]+)""""
  lazy val baseUrl_Auth_Token: String   = baseUrlFor("auth-retrieve-token")
  lazy val jsonPattern: UnanchoredRegex = """\{"\w+":"([^"]+)""".r.unanchored

  lazy val clientId             = sys.env("CLIENT_ID")
  lazy val clientSecret: String = sys.env("CLIENT_SECRET")
  lazy val redirectUri: String  = "urn:ietf:wg:oauth:2.0:oob"
  lazy val authBaseUrl: String  = baseUrlFor("auth-login-stub")

  lazy val authUrl: String = s"$authBaseUrl/auth-login-stub/gg-sign-in"
  lazy val redirectionUrl  = s"$authBaseUrl/auth-login-stub/session"
  lazy val authUrlLocal: String = s"$authBaseUrl/government-gateway/session/login"
  lazy val scope: String   = "tax-free-childcare-payments"

  val postAuthApiSessionLogin: HttpRequestBuilder =
    http("Post to Auth API Session Login")
      .post(authUrlLocal)
      .body(StringBody(authPayload("${nino}")))
      .header("Content-Type", "application/json")
      .check(saveAuthBearerToken)

  def saveAuthBearerToken: CheckBuilder[HttpHeaderCheckType, Response, String] =
    header(HttpHeaderNames.Authorization).saveAs("authBearerToken")

  val postLink: HttpRequestBuilder =
    http("Payment Link Request")
      .post(s"$baseUrl/link")
      .header("Authorization", s"Bearer $bearerToken")
      .header("Content-Type", "application/json")
      .header("Accept", "application/vnd.hmrc.1.2+json")
      .header("Correlation-ID", "${correlationId}")
      .header("Referer", "")
      .body(StringBody(linkPayload("${eppUniqueCustomerId}","${eppRegReference}","${outboundChildPaymentRef}","${childDateOfBirth}")))
      .asJson
      .check(status.is(200))

  val postBalance: HttpRequestBuilder =
    http("Post balance endpoint")
      .post(s"$baseUrl/balance")
      .header("Content-Type", "application/json")
      .header("Accept", "application/vnd.hmrc.1.2+json")
      .header("Authorization", s"Bearer $bearerToken")
      .header("Correlation-ID", "${correlationId}")
      .header("Referer", "")
      .body(StringBody(balancePayload("${eppUniqueCustomerId}","${eppRegReference}","${outboundChildPaymentRef}")))
      .check(status.is(200))

  val postPayment: HttpRequestBuilder =
    http("Post payment endpoint")
      .post(s"$baseUrl")
      .header("Content-Type", "application/json")
      .header("Accept", "application/vnd.hmrc.1.2+json")
      .header("Authorization", s"Bearer $bearerToken")
      .header("Correlation-ID", "${correlationId}")
      .header("Referer", "")
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
       | "payment_amount":$paymentAmount,
       | "ccp_reg_reference": "$ccpRegReference",
       | "ccp_postcode": "$ccpPostcode",
       | "payee_type": "$payeeType",
       | "outbound_child_payment_ref": "$outboundChildPaymentRef"
       | }
    """.stripMargin

  def saveCsrfToken(): CheckBuilder[RegexCheckType, String, String] = regex(_ => CsrfPattern).saveAs("csrfToken")

  def getAuthId: HttpRequestBuilder =
    http(requestName = "get AuthId")
      .get(
        s"$authBaseUrl/oauth/authorize?client_id=$clientId&redirect_uri=$redirectUri&scope=$scope&response_type=code"
      )
      .check(status.is(expected = 303))
      .check(
        header("Location")
          .transform(location => extractDynamicCode(location))
          .saveAs("auth_id")
      )
      .check(header("Location").is("/oauth/start?auth_id=${auth_id}"))

  def postAuthLogin(): HttpRequestBuilder =
    http("Enter Auth login credentials ")
      .post(authUrl)
      .formParam("redirectionUrl", redirectionUrl)
      .formParam("authorityId", "")
      .formParam("credentialStrength", "strong")
      .formParam("confidenceLevel", "250")
      .formParam("affinityGroup", "Individual")
      .formParam("enrolments","[]")
      .formParam("nino", "${nino}")
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

  def extractDynamicCode(location: String): String = {
    val p = """([^=]*)$""".r.unanchored
    location match {
      case p(auth_id) => auth_id
      case _          => ""
    }
  }

  def getStart: HttpRequestBuilder =
    http("get Start")
      .get(authBaseUrl + "/oauth/start?auth_id=${auth_id}")
      .check(status.is(200))

  def getGrantAuthority303: HttpRequestBuilder =
    http("get Grant Authority 303")
      .get(authBaseUrl + "/oauth/grantscope?auth_id=${auth_id}")
      .check(status.is(303))
      .check(
        header("Location")
          .is(authBaseUrl + "/gg/sign-in?continue=%2Foauth%2Fgrantscope%3Fauth_id%3D${auth_id}&origin=oauth-frontend"+s"&clientId=$clientId")
      )

  def getCredentialsPage: HttpRequestBuilder =
    http("get credentials page")
      .get(authBaseUrl + "/auth-login-stub/gg-sign-in")
      .check(status.is(200))

  def authLogin(): HttpRequestBuilder =
    http("login Step")
      .post(authBaseUrl + "/auth-login-stub/gg-sign-in")
      .formParam("redirectionUrl", s"$authBaseUrl/oauth/authorize?client_id=$clientId&redirect_uri=$redirectUri&scope=$scope&response_type=code")
      .formParam("credentialStrength", "strong")
      .formParam("confidenceLevel", "250")
      .formParam("authorityId", "${nino}")
      .formParam("affinityGroup", "Individual")
      .formParam("enrolments","[]")
      .formParam("nino", "${nino}")
      .check(status.is(303))

  def grantAuthorityRedirect: HttpRequestBuilder =
    http("get Grant Authority 2nd Redirect")
      .get(authBaseUrl + "/gg/sign-in?continue=%2Foauth%2Fgrantscope%3Fauth_id%3D${auth_id}&origin=oauth-frontend")
      .check(status.is(303))
      .check(
        header("Location")
          .is(
            authBaseUrl + "/bas-gateway/sign-in?continue_url=%2Foauth%2Fgrantscope%3Fauth_id%3D${auth_id}&origin=oauth-frontend"
          )
      )

  def grantAuthorityRedirect2: HttpRequestBuilder =
    http("get Grant Authority  Redirect2")
      .get(
        authBaseUrl + "/bas-gateway/sign-in?continue_url=%2Foauth%2Fgrantscope%3Fauth_id%3D${auth_id}&origin=oauth-frontend"
      )
      .check(status.is(303))

  def getGrantAuthority200: HttpRequestBuilder =
    http("get Grant Authority 200")
      .get(authBaseUrl + "/oauth/grantscope?auth_id=${auth_id}")
      .check(status.is(200))
      .check(saveCsrfToken())

  def submitGrantAuthority: HttpRequestBuilder =
    http("submit Grant Authority")
      .post(authBaseUrl + "/oauth/grantscope": String)
      .formParam("csrfToken", "${csrfToken}")
      .formParam("auth_id", "${auth_id}")
      .check(status.is(200))
      .check(css("#authorisation-code").ofType[String].saveAs("code"))

  def getAccessTokenGG: HttpRequestBuilder =
    http("Retrieve Access Token through GG")
      .post(s"$baseUrl_Auth_Token/oauth/token")
      .headers(Map("Content-Type" -> "application/x-www-form-urlencoded"))
      .body(
        StringBody(
          "code=${code}&client_id=" + clientId +
            "&client_secret=" + clientSecret +
            "&grant_type=authorization_code&redirect_uri=" + redirectUri
        )
      )
      .check(
        bodyString
          .transform { (body: String) =>
            body match {
              case jsonPattern(access_token) => access_token
              case _                         => ""
            }
          }
          .saveAs("accessToken")
      )
      .check(status.is(200))

}
