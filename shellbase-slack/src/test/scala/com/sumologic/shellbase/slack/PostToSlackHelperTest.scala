/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sumologic.shellbase.slack

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PostToSlackHelperTest extends CommonWordSpec {
  // NOTE: Some of the test coverage for PostToSlackHelper is done by PostCommandToSlackTest

  "PostToSlackHelper" should {
    "skip posting if username is blacklisted" in {
      val sut = new PostToSlackHelper {
        override protected val slackState: SlackState = null
        override protected val username = "my_test"
        override protected val blacklistedUsernames = Set("my_test", "my_test_2")
      }

      sut.sendSlackMessageIfConfigured("")
    }

    "allow posting if username is not blacklist" in {
      val sut = new PostToSlackHelper {
        override protected val slackState: SlackState = null
        override protected val username = "abc"
        override protected val blacklistedUsernames = Set("my_test", "my_test_2")
      }

      intercept[NullPointerException] {
        sut.sendSlackMessageIfConfigured("")
      }
    }
  }

}
