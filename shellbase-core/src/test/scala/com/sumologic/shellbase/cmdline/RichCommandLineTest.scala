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
package com.sumologic.shellbase.cmdline

import com.sumologic.shellbase.CommonWordSpec
import com.sumologic.shellbase.cmdline.RichCommandLine._
import org.apache.commons.cli.Options
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RichCommandLineTest extends CommonWordSpec {

  "Command line options" should {
    "return a default value if no value was provided on the command line" in {

      val defaultValue = "blargh"
      val sut = new CommandLineOption("s", "test", true, "halp", Some(defaultValue))

      val options = new Options
      options += sut

      val cmdLine = Array[String]().parseCommandLine(options)
      cmdLine.get.get(sut).get should equal(defaultValue)
    }

    "return a provided value if one was provided despite a default" in {

      val defaultValue = "blargh"
      val sut = new CommandLineOption("s", "test", true, "halp", Some(defaultValue))

      val options = new Options
      options += sut

      val providedValue = "wtf?"
      val cmdLine = Array[String]("-s", providedValue).parseCommandLine(options)
      cmdLine.get.get(sut).get should equal(providedValue)
    }

    "not accept options with the same short name" in {
      val options = new Options
      options += new CommandLineOption("s", "one", true, "same shit")
      the[IllegalArgumentException] thrownBy {
        options += new CommandLineOption("s", "two", true, "different day")
      }
    }

    "not accept options with the same long name" in {
      val options = new Options
      options += new CommandLineOption("x", "same", true, "same shit")
      the[IllegalArgumentException] thrownBy {
        options += new CommandLineOption("y", "same", true, "different day")
      }
    }
  }

  "RichCommandLine.apply" should {
    "return a provided value" in {
      val sut = new CommandLineOption("a", "animal", false, "halp")

      val options = new Options
      options += sut

      val providedValue = "wombat"
      val cmdLine = Array[String]("-a", providedValue).parseCommandLine(options).get
      cmdLine(sut) should equal(providedValue)
    }

    "throw a NoSuchElementException for a missing command line parameter" in {
      val sut = new CommandLineOption("a", "animal", false, "halp")
      val anotherOption = new CommandLineOption("ml", "my-love", true, "here I am!")

      val options = new Options
      options += sut
      options += anotherOption

      val cmdLine = Array[String]("--my-love", "wombat").parseCommandLine(options).get
      a[NoSuchElementException] should be thrownBy {
        cmdLine(sut)
      }
    }
  }

}
