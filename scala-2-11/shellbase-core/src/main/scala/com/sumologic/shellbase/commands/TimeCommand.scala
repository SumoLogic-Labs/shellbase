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

import com.sumologic.shellbase.ShellCommand
import com.sumologic.shellbase.cmdline.CommandLineArgument
import com.sumologic.shellbase.cmdline.RichCommandLine._
import com.sumologic.shellbase.timeutil.TimeFormats
import org.apache.commons.cli.{CommandLine, Options}

class TimeCommand(runCommand: String => Boolean) extends ShellCommand("time", "Measure the execution time of a command") {

  private val CommandArgument = new CommandLineArgument("command", 0, true)

  private def now = System.currentTimeMillis()

  override def maxNumberOfArguments = 1

  override def addOptions(opts: Options) {
    opts += CommandArgument
  }

  import com.sumologic.shellbase.ShellBase.SubCommandExtractor

  def execute(cmdLine: CommandLine) = {
    cmdLine.get(CommandArgument) match {
      case Some(SubCommandExtractor(cmd)) =>
        val start = now
        val exitStatus = runCommand(cmd)
        val dt = now - start
        val dtMessage = s"Execution took $dt ms (${TimeFormats.formatAsTersePeriod(dt)})"
        _logger.info(s"$dtMessage for `$cmd`")
        println(s"\n$dtMessage\n")
        exitStatus
      case badCmd =>
        println(s"Usage: time `<command>`, but found $badCmd.")
        false
    }
  }
}
