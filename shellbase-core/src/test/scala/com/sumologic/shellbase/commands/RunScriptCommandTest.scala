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

import java.io.File

import com.sumologic.shellbase.CommonWordSpec
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RunScriptCommandTest extends CommonWordSpec {
  "RunScriptCommand" should {
    "handle non-existent script files" in {
      val sut = new RunScriptCommand(List(scriptsDir), null, runCommand = _ => throw new Exception("Should not get here"))
      sut.executeLine(List.empty) should be (false)

      sut.executeLine(List("does_not_exist")) should be (false)
    }

    "run scripts with short names" in {
      import java.nio.file.Files
      import java.io.File

      def from(n: Int): Stream[Int] = n #:: from(n + 1)

      val nats: Seq[Int] = from(0)

      val tmpDir = new File(System.getProperty("java.io.tmpdir"))
      val tmpFileOpt = nats.take(100).map(n => new File(tmpDir, n.toString)).find(f => !f.exists())

      tmpFileOpt match {
        case Some(tmpFile) =>
          tmpFile.deleteOnExit()
          Files.write(tmpFile.toPath, "echo hello".getBytes())

          val sut = new RunScriptCommand(List(scriptsDir), "", runCommand = _ => true)
          sut.executeLine(List(tmpFile.getAbsolutePath)) should be(true)

        case None => fail("Can't create unique tmp file")
      }
    }

    // FIXME(chris, 2016-05-25): These tests does not pass as the path resolution ignores scriptDir.  I need to fix that
    // first, but it's a larger task than I want to do right this second.  Additionally, until we unify that code, we should
    // skip writing tests for auto-complete.

    "accept either scripts or the parent of scripts dir" ignore {
      val sut1 = new RunScriptCommand(List(scriptsDir), null, runCommand = inputShouldBeSimple(true))
      sut1.executeLine(List("simple")) should be (true)

      val sut2 = new RunScriptCommand(List(scriptsDir.getParentFile), null, runCommand = inputShouldBeSimple(true))
      sut2.executeLine(List("simple")) should be (true)
    }

    "return the status of runCommand" ignore {
      val sut1 = new RunScriptCommand(List(scriptsDir), null, runCommand = inputShouldBeSimple(false))
      sut1.executeLine(List("simple")) should be (false)

      val sut2 = new RunScriptCommand(List(scriptsDir), null, runCommand = inputShouldBeSimple(true))
      sut2.executeLine(List("simple")) should be (true)
    }
  }

  private def inputShouldBeSimple(ret: Boolean = true)(cmd: String): Boolean = {
    cmd should be ("hi")
    ret
  }

  private val scriptsDir = new File("src/test/resources/scripts")

}
