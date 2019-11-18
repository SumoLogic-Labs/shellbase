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

import com.sumologic.shellbase.cmdline.RichCommandLine._
import com.sumologic.shellbase.cmdline.{CommandLineOption, RichCommandLine}
import org.apache.commons.cli.{CommandLine, Options}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ShellCommandAliasTest extends CommonWordSpec {
  "ShellCommandAlias" should {
    "execute original command" in {
      val cmd = new DummyCommand("almond")
      val subtree = new ShellCommandAlias(cmd, "better_almond", List())
      subtree.execute(null)

      cmd.executed should be(true)
    }

    "have same options" in {
      val option = new CommandLineOption("e", "example", true, "An example in test")
      val cmd = new ShellCommand("almond", "Just for testing") {

        def execute(cmdLine: CommandLine) = {
          cmdLine.get(option).isDefined
        }

        override def addOptions(opts: Options) {
          opts.addOption(option)
        }
      }
      val subtree = new ShellCommandAlias(cmd, "alias", List())

      subtree.parseOptions(List("--example", "a")).hasOption("example") should be(true)
      cmd.parseOptions(List("--example", "a")).hasOption("example") should be(true)
      val cmdLine = mock(classOf[CommandLine])
      when(cmdLine.hasOption("example")).thenReturn(true)
      when(cmdLine.getOptionValue("example")).thenReturn("return")
      subtree.execute(cmdLine)
    }
  }
}
