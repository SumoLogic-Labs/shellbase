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

import java.io.File
import java.nio.file.Files

import com.sumologic.shellbase.ShellPromptValidators._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ShellPromptValidatorsTest extends CommonWordSpec {
  "ShellPromptValidators.empty" should {
    "always figure out string is empty" in {
      ShellPromptValidators.empty("").valid should be(true)
      ShellPromptValidators.empty("abc").valid should be(false)
    }
  }

  "ShellPromptValidators.nonEmpty" should {
    "figure out if string is not empty" in {
      nonEmpty("abc").valid should be(true)
      nonEmpty("").valid should be(false)
    }
  }

  "ShellPromptValidators.numeric" should {
    "always figure out string is integer" in {
      numeric("123").valid should be(true)
      numeric("-1234").valid should be(true)
      numeric("abc").valid should be(false)
      numeric("12abc").valid should be(false)
      numeric("--1234").valid should be(false)
      numeric("").valid should be(false)
    }
  }

  "ShellPromptValidators.isFloatingNumber" should {
    "always figure out string is floating number" in {
      isFloatingNumber("0.123").valid should be(true)
      isFloatingNumber("78.12").valid should be(true)
      isFloatingNumber("-78.12").valid should be(true)
      isFloatingNumber("abc").valid should be(false)
      isFloatingNumber("1.2abc").valid should be(false)
      isFloatingNumber("").valid should be(false)
    }
  }

  "ShellPromptValidators.inNumberRange" should {
    "always test string is integer and in integer number range" in {
      inNumberRange(10, 20)("15").valid should be(true)
      inNumberRange(-10, 20)("0").valid should be(true)
      inNumberRange(10, 20)("25").valid should be(false)
      inNumberRange(10, 20)("abc").valid should be(false)
      inNumberRange(10, 20)("12.0").valid should be(false)
      inNumberRange(10, 20)("10").valid should be(true)
      inNumberRange(10, 20)("20").valid should be(true)
    }
  }

  "ShellPromptValidators.inTimeRange" should {
    "always test string is a time period and in long number range" in {
      val minutes10 = 10 * 60 * 1000l
      val minutes20 = minutes10 * 2
      val hour1 = minutes10 * 6

      inTimeRange(minutes10, minutes20)("15m").valid should be(true)
      inTimeRange(minutes10, minutes20)("900s").valid should be(true)
      inTimeRange(minutes10, minutes20)("900000").valid should be(true)
      inTimeRange(0, 20)("0").valid should be(true)
      inTimeRange(0, 20)("0h").valid should be(true)
      inTimeRange(0, 20)("0m").valid should be(true)
      inTimeRange(0, 20)("0s").valid should be(true)
      inTimeRange(0, hour1)("1h").valid should be(true)
      inTimeRange(0, hour1)("60m").valid should be(true)
      inTimeRange(0, hour1)("60000s").valid should be(false)
      inTimeRange(0, hour1)("2h").valid should be(false)
      inTimeRange(10, 20)("25").valid should be(false)
      inTimeRange(10, 20)("abc").valid should be(false)
      inTimeRange(10, 20)("12.0").valid should be(false)
      inTimeRange(10, 20)("12.0m").valid should be(false)
      inTimeRange(10, 20)("10").valid should be(true)
      inTimeRange(10, 20)("20").valid should be(true)
    }
  }

  "ShellPromptValidators.inNumberDoubleRange" should {
    "always test string is in double number range" in {
      inNumberDoubleRange(0.0, 1.0)("0.5").valid should be(true)
      inNumberDoubleRange(-1.0, 1.0)("0").valid should be(true)
      inNumberDoubleRange(1.0, 2.0)("2.5").valid should be(false)
      inNumberDoubleRange(1.0, 2.0)("abc").valid should be(false)
      inNumberDoubleRange(10, 20)("2.0").valid should be(false)
      inNumberDoubleRange(1.0, 2.0)("1.0").valid should be(true)
      inNumberDoubleRange(1.0, 2.0)("2.0").valid should be(true)
    }
  }

  "ShellPromptValidators.positiveInteger" should {
    "always test string is positive number" in {
      positiveInteger("123").valid should be(true)
      positiveInteger("123.0").valid should be(false)
      positiveInteger("-1234").valid should be(false)
      positiveInteger("abc").valid should be(false)
      positiveInteger("12abc").valid should be(false)
      positiveInteger("--1234").valid should be(false)
      positiveInteger("").valid should be(false)
    }
  }

  "ShellPromptValidators.inList" should {
    "always test string is in string list" in {
      inList(List("123", "abc", "test"))("test").valid should be(true)
      inList(List("123", "abc", "test"))("astc").valid should be(false)
      inList(List("", "1234"))("").valid should be(true)
      inList(List("wert", "1234"))("").valid should be(false)
    }
  }

  "ShellPromptValidators.notInList" should {
    "always test string is not in string list" in {
      notInList(List("123", "abc", "test"))("test").valid should be(false)
      notInList(List("123", "abc", "test"))("astc").valid should be(true)
      notInList(List("", "1234"))("").valid should be(false)
      notInList(List("wert", "1234"))("").valid should be(true)
    }
  }

  "ShellPromptValidators.isURL" should {
    "accept valid urls" in {
      isURL()("http://sumologic.com").valid should be (true)
      isURL()("http://www.sumologic.com").valid should be (true)
      isURL()("https://sumologic.com").valid should be (true)
      isURL()("https://www.sumologic.com").valid should be (true)
      isURL()("http://sumologic.com").valid should be (true)
      isURL()("https://service.us2.sumologic.com").valid should be (true)
    }

    "fail invalid urls" in {
      isURL()("asdf://sumologic.com").valid should be (false)
      isURL()("adsfj901je").valid should be (false)
    }
  }

  "ShellPromptValidators.existingFile and ShellPromptValidators.nonExistingFile" should {
    "always determine if file exists" in {
      val file = Files.createTempFile("exists", "")
      val nobodyFile = new File("/tmp/doesnt/exist")
      existingFile()(file.toString).valid should be(true)
      existingFile()(nobodyFile.toString).valid should be(false)
      nonExistingFile()(file.toString).valid should be(false)
      nonExistingFile()(nobodyFile.toString).valid should be(true)
    }
  }

  "ShellPromptValidators.lengthBetween" should {
    "always determine length between" in {
      lengthBetween(0, 1)("").valid should be(true)
      lengthBetween(0, 1)("a").valid should be(true)
      lengthBetween(0, 1)("ab").valid should be(false)

      lengthBetween(1, 2)("").valid should be(false)
    }
  }

  "ShellPromptValidators.onlyLowerCase, onlyLetters, onlyNumbers" should {
    "always determine character set" in {
      val alphabet = "abcdefghijklmnopqrstuvwxyz"

      onlyLowerCase(alphabet).valid should be(true)
      onlyLowerCase("A").valid should be(false)

      onlyLetters(alphabet).valid should be(true)
      onlyLetters(alphabet.toUpperCase).valid should be(true)
      onlyLetters("0").valid should be(false)

      onlyNumbers(alphabet).valid should be(false)
      onlyNumbers(alphabet.toUpperCase()).valid should be(false)
      onlyNumbers("0123456789").valid should be(true)
    }
  }

  "ShellPromptValidators.or" should {
    "correctly pass result down to children validators" in {
      val result = "test_me"
      var count = 0

      def testResult(res: String): ValidationResult = {
        res should be (result)
        count += 1
        ValidationFailure("failure")
      }

      ShellPromptValidators.or(testResult _, testResult _)(result).valid should be (false)
      count should be (2)
    }

    "always determine or correctly" in {
      ShellPromptValidators.or(_ => ValidationSuccess, _ => ValidationSuccess)("").valid should be(true)
      ShellPromptValidators.or(_ => ValidationFailure("failure"), _ => ValidationSuccess)("").valid should be(true)
      ShellPromptValidators.or(_ => ValidationSuccess, _ => ValidationFailure("failure"))("").valid should be(true)
      ShellPromptValidators.or(_ => ValidationFailure("failure"), _ => ValidationFailure("failure"))("").valid should be(false)
    }
  }

  "ShellPromptValidators.listOf" should {
    "only pass if all pass" in {
      listOf(numeric)("1,2,3,4").valid should be (true)
    }

    "proces each element only once" in {
      var failureCount = 0
      def numericProxy(result: String): ValidationResult = {
        val vResult = numeric(result)
        if (!vResult.valid) {
          failureCount += 1
        }
        vResult
      }

      listOf(numericProxy)("1,2,a,b,c").valid should be (false)
      failureCount should be (3)
    }
  }
}
