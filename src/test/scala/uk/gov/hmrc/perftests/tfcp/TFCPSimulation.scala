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

import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import uk.gov.hmrc.perftests.tfcp.TFCPRequests._

class TFCPSimulation extends PerformanceTestRunner {

  setup("post-auth-login", "Post auth stub login") withRequests postAuthApiSessionLogin

  setup("post-Link", "post TFCP Link") withRequests postLink

  setup("post-Balance", "post TFCP Balance") withRequests postBalance

  setup("post-Payment", "post TFCP Payment") withRequests postPayment

  runSimulation()
}
