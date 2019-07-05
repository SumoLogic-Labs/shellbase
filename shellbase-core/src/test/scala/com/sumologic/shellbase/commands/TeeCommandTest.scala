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
package com.sumologic.shellbase.commands

import java.nio.charset.Charset
import java.nio.file.{Files, Path}

import com.sumologic.shellbase.CommonWordSpec
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.collection.JavaConverters._
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class TeeCommandTest extends CommonWordSpec {
  "TeeCommand" should {
    "execute a subcommand and propagate exit code" in {
      var calls = 0
      def callCheck(ret: Boolean)(input: String): Boolean = {
        input should be("hi")
        calls += 1
        ret
      }

      new TeeCommand(callCheck(true)).executeLine(List("`hi`", "-o", getTempFilePath().toString)) should be(true)
      calls should be(1)

      new TeeCommand(callCheck(false)).executeLine(List("`hi`", "-o", getTempFilePath().toString)) should be(false)
      calls should be(2)
    }

    "degrade nicely with malformatted input" in {
      new TeeCommand(_ => true).executeLine(List.empty) should be(false)
      new TeeCommand(_ => true).executeLine(List("test")) should be(false)
    }

    "write output to file, and support append mode" in {
      def printMessage(str: String): Boolean = {
        println(str)
        true
      }

      val tempFile = getTempFilePath()
      new TeeCommand(printMessage).executeLine(List("`hi mom`", "-o", tempFile.toString))
      // The first line is the debug line, so everything after is logged
      readTempFile(tempFile) should be(List("hi mom"))

      // We should override since not in append mode
      new TeeCommand(printMessage).executeLine(List("`hi mom 2`", "-o", tempFile.toString))
      // The first line is the debug line, so everything after is logged
      readTempFile(tempFile) should be(List("hi mom 2"))

      // We have both 2 and 3 since in append move
      new TeeCommand(printMessage).executeLine(List("`hi mom 3`", "-o", tempFile.toString, "-a"))
      // The first line is the debug line, so everything after is logged
      readTempFile(tempFile) should be(List("hi mom 2", "hi mom 3"))
    }


  }

  private def getTempFilePath(): Path = {
    Files.createTempFile("teecommand", ".tmp")
  }

  private def readTempFile(path: Path): List[String] = {
    Files.readAllLines(path, Charset.defaultCharset()).asScala.filterNot(_.startsWith("Running")).toList
  }

}
