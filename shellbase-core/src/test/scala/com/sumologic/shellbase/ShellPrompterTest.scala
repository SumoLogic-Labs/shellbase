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

/**
  * Created by sining on 1/5/16.
  */
@RunWith(classOf[JUnitRunner])
class ShellPrompterTest extends CommonWordSpec {
  "ShellPrompterValidator" should {
    "always figure out string is not empty" in {
      nonEmpty("abc").valid should be(true)
      nonEmpty("").valid should be(false)
    }

    "always figure out string is empty" in {
      ShellPromptValidators.empty("").valid should be(true)
      ShellPromptValidators.empty("abc").valid should be(false)
    }

    "always figure out string is integer" in {
      numeric("123").valid should be(true)
      numeric("-1234").valid should be(true)
      numeric("abc").valid should be(false)
      numeric("12abc").valid should be(false)
      numeric("--1234").valid should be(false)
      numeric("").valid should be(false)
    }

    "always figure out string is floating number" in {
      isFloatingNumber("0.123").valid should be(true)
      isFloatingNumber("78.12").valid should be(true)
      isFloatingNumber("-78.12").valid should be(true)
      isFloatingNumber("abc").valid should be(false)
      isFloatingNumber("1.2abc").valid should be(false)
      isFloatingNumber("").valid should be(false)
    }

    "always test string is integer and in integer number range" in {
      inNumberRange(10, 20)("15").valid should be(true)
      inNumberRange(-10, 20)("0").valid should be(true)
      inNumberRange(10, 20)("25").valid should be(false)
      inNumberRange(10, 20)("abc").valid should be(false)
      inNumberRange(10, 20)("12.0").valid should be(false)
      inNumberRange(10, 20)("10").valid should be(true)
      inNumberRange(10, 20)("20").valid should be(true)
    }

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

    "always test string is in double number range" in {
      inNumberDoubleRange(0.0, 1.0)("0.5").valid should be(true)
      inNumberDoubleRange(-1.0, 1.0)("0").valid should be(true)
      inNumberDoubleRange(1.0, 2.0)("2.5").valid should be(false)
      inNumberDoubleRange(1.0, 2.0)("abc").valid should be(false)
      inNumberDoubleRange(10, 20)("2.0").valid should be(false)
      inNumberDoubleRange(1.0, 2.0)("1.0").valid should be(true)
      inNumberDoubleRange(1.0, 2.0)("2.0").valid should be(true)
    }

    "always test string is positive number" in {
      positiveInteger("123").valid should be(true)
      positiveInteger("123.0").valid should be(false)
      positiveInteger("-1234").valid should be(false)
      positiveInteger("abc").valid should be(false)
      positiveInteger("12abc").valid should be(false)
      positiveInteger("--1234").valid should be(false)
      positiveInteger("").valid should be(false)
    }

    "always test string is in string list" in {
      inList(List("123", "abc", "test"))("test").valid should be(true)
      inList(List("123", "abc", "test"))("astc").valid should be(false)
      inList(List("", "1234"))("").valid should be(true)
      inList(List("wert", "1234"))("").valid should be(false)
    }

    "always test string is not in string list" in {
      notInList(List("123", "abc", "test"))("test").valid should be(false)
      notInList(List("123", "abc", "test"))("astc").valid should be(true)
      notInList(List("", "1234"))("").valid should be(false)
      notInList(List("wert", "1234"))("").valid should be(true)
    }

    "always determine if file exists" in {
      val file = Files.createTempFile("exists", "")
      val nobodyFile = new File("/tmp/doesnt/exist")
      existingFile()(file.toString).valid should be (true)
      existingFile()(nobodyFile.toString).valid should be (false)
      nonExistingFile()(file.toString).valid should be (false)
      nonExistingFile()(nobodyFile.toString).valid should be (true)
    }

    "always determine character set" in {
      val alphabet = "abcdefghijklmnopqrstuvwxyz"

      onlyLowerCase(alphabet).valid should be (true)
      onlyLowerCase("A").valid should be (false)

      onlyLetters(alphabet).valid should be (true)
      onlyLetters(alphabet.toUpperCase).valid should be (true)
      onlyLetters("0").valid should be (false)

      onlyNumbers(alphabet).valid should be (false)
      onlyNumbers(alphabet.toUpperCase()).valid should be (false)
      onlyNumbers("0123456789").valid should be (true)
    }
  }
}
