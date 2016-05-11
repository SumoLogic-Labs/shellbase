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

import jline.console.ConsoleReader
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ShellTest extends CommonWordSpec with MockitoSugar {
  "ShellPrompter" should {
    "validate questions" in {
      val input = mock[ConsoleReader]
      val question = "Do you like testing?"
      val answer = "answer"

      when(input.readLine(contains(question), isNull(classOf[Character]))).thenReturn(answer)

      val prompter = new ShellPrompter(input)
      prompter.askQuestion(question, List(ShellPromptValidators.nonEmpty)) should equal(answer)
    }
  }

  "ShellPromptValidators" should {
    "throw exception for invalid options for chooseNCommaSeparated" in {
      intercept[IllegalArgumentException] {
        ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 4)("1,2")
      }
    }

    "validate as success for valid result for chooseNCommaSeparated" in {
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1,2").valid shouldBe true
    }

    "validate as failure for invalid result for chooseNCommaSeparated" in {
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1").valid shouldBe false
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1,4").valid shouldBe false
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1,4,5").valid shouldBe false
    }

    "accept spaces around commas in chooseNCommaSeparated" in {
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1, 2").valid shouldBe true
    }
  }
}
