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
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ShellCommandSetTest extends CommonWordSpec {
  "A ShellCommandSet" should {

    "always accept the help command" in {
      val sut = new ShellCommandSet("test", "set help text")
      sut.commands += new TestCommand("one")
      run(sut, "test help") should be(true)
      run(sut, "test ?") should be(true)
      run(sut, "test ? one") should be(true)
      run(sut, "test ? two") should be(false)
      run(sut, "test") should be(true)
    }

    "execute commands in the set" in {
      val sut = new ShellCommandSet("test", "set help text")
      val one = new TestCommand("one")
      val two = new TestCommand("two")
      sut.commands += one
      sut.commands += two

      run(sut, "test one") should be(true)
      one.commandLines.size should be(1)
      two.commandLines.size should be(0)
    }

    "support nested sets" in {
      val sut = new ShellCommandSet("test", "set help text")
      val level = new ShellCommandSet("level", "level help text")
      sut.commands += level
      val one = new TestCommand("one")
      val two = new TestCommand("two")
      level.commands += one
      level.commands += two

      run(sut, "test level one") should be(true)
      one.commandLines.size should be(1)
      two.commandLines.size should be(0)
    }

    "return false" when {

      "the command does not exist" in {
        val sut = new ShellCommandSet("test", "set help text")
        run(sut, "test say-what") should be(false)
      }

      "the command called returns false" in {
        val sut = new ShellCommandSet("test", "set help text")
        val one = new TestCommand("one")
        one.returnResult = false
        sut.commands += one
        run(sut, "test one") should be(false)
        one.commandLines.size should be(1)
      }

      "the command called throws a Throwable" in {
        val sut = new ShellCommandSet("test", "set help text")
        val one = new TestCommand("one") {
          override def execute(cmdLine: CommandLine) = {
            super.execute(cmdLine)
            throw new Throwable("urgh!")
          }
        }
        sut.commands += one
        run(sut, "test one") should be(false)
        one.commandLines.size should be(1)
      }

      "the command fails preExecute" in {
        val sut = new ShellCommandSet("test", "set help text")

        val throwExceptionShellHook: ShellCommandSet.ExecuteHook = (_, _) => throw new Exception("test")
        sut.preExecuteHooks += throwExceptionShellHook

        val one = new TestCommand("one")
        one.returnResult = true
        sut.commands += one
        run(sut, "test one") should be(false)
        one.commandLines.size should be(0)
      }

      "the command fails postExecute" in {
        val sut = new ShellCommandSet("test", "set help text")

        val throwExceptionShellHook: ShellCommandSet.ExecuteHook = (_, _) => throw new Exception("test")
        sut.postExecuteHooks += throwExceptionShellHook

        val one = new TestCommand("one")
        one.returnResult = true
        sut.commands += one
        run(sut, "test one") should be(false)
        one.commandLines.size should be(1)
      }
    }

    "not run a command" when {
      "validation fails on the set" in {
        val sut = new ShellCommandSet("test", "set help text") with FailedValidation
        val one = new TestCommand("one")
        sut.commands += one
        val result = run(sut, "test one")
        result should be(false)
        one.commandLines.size should be(0)
      }

      "validation fails on the command" in {
        val sut = new ShellCommandSet("test", "set help text")
        val one = new TestCommand("one") with FailedValidation
        sut.commands += one
        val result = run(sut, "test one")
        result should be(false)
        one.commandLines.size should be(0)
      }
    }

    "not allow running old execute line" in {
      val sut = new ShellCommandSet("blah", "")
      intercept[IllegalAccessException] {
        sut.execute(null)
      }
    }
  }

  def run(sut: ShellCommandSet, command: String): Boolean = {
    val root = new ShellCommandSet("", "root help text")
    root.commands += sut
    root.executeLine(command.split(" ").toList)
  }
}

class TestCommand(name: String) extends ShellCommand(name, "test command " + name) {

  var commandLines = List[CommandLine]()

  var returnResult = true

  def execute(cmdLine: CommandLine) = {
    commandLines +:= cmdLine
    returnResult
  }
}

trait FailedValidation extends ShellCommand {
  override def validate(cmdLine: CommandLine) = {
    Some("failure")
  }
}
