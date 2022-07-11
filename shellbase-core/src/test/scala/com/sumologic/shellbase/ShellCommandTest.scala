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
package com.sumologic.shellbase

import org.apache.commons.cli.CommandLine
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ShellCommandTest extends CommonWordSpec {
  "ShellCommand" should {
    "parse comments" in {
      sut().extractPossibleComments(List("")) shouldBe((List(""), None))
      sut().extractPossibleComments(List("foo","show", "-bar", "-p", "a;c:1")) shouldBe(
        (List("foo", "show", "-bar", "-p", "a;c:1"), None))
      sut().extractPossibleComments(List("live", "a", "happy", "life")) shouldBe(
        (List("live", "a", "happy", "life"), None))
      sut().extractPossibleComments(List("live", "a", "happy", "life", "#", "and", "don't", "regret", "anything")) shouldBe(
        (List("live", "a", "happy", "life"), Some("and don't regret anything")))
    }
  }

  def sut() = {
    new ShellCommand("do", "Does the thing") {
      override def execute(cmdLine: CommandLine): Boolean = true
    }
  }
}
