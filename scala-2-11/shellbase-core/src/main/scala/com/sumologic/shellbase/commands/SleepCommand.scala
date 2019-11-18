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
import com.sumologic.shellbase.cmdline.RichCommandLine._
import com.sumologic.shellbase.cmdline.{CommandLineArgument, CommandLineFlag}
import com.sumologic.shellbase.timeutil.TimeFormats
import org.apache.commons.cli.{CommandLine, Options}

class SleepCommand extends ShellCommand("sleep", "Sleeps the specified time period.", List("zzz")) {

  override def maxNumberOfArguments = 1

  private def now = System.currentTimeMillis()

  def execute(cmdLine: CommandLine) = {
    val showSleepIndicator = cmdLine.checkFlag(ShowIndicatorFlag)

    cmdLine.get(PeriodArgument) match {
      case Some(period) =>
        val milliseconds = TimeFormats.parseTersePeriod(period).
          getOrElse(throw new IllegalArgumentException(s"Could not parse $period"))
        val canonicalPeriod = TimeFormats.formatWithMillis(milliseconds)
        println(s"Sleeping $canonicalPeriod.")

        val startTime = now
        var timeRemaining = milliseconds
        while (timeRemaining > 0) {
          try {
            Thread.sleep(1000L min timeRemaining)
          } catch {
            case ie: InterruptedException => _logger.debug("Error sleeping", ie)
          }
          if (showSleepIndicator) {
            print(".")
          }
          timeRemaining = startTime + milliseconds - now
        }
        if (showSleepIndicator) {
          println()
        }
        true

      case None =>
        println("Missing argument to sleep command!")
        false
    }
  }

  private val PeriodArgument = new CommandLineArgument("period", 0, true)
  private val ShowIndicatorFlag = new CommandLineFlag("v", "verbose", "whether to print . every second while sleeping")

  override def addOptions(opts: Options) {
    opts += PeriodArgument
    opts += ShowIndicatorFlag
  }
}
