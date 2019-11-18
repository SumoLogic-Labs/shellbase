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
package com.sumologic.shellbase.notifications

import com.sumologic.shellbase.cmdline.RichCommandLine._
import com.sumologic.shellbase.cmdline.CommandLineArgument
import com.sumologic.shellbase.table.ASCIITable
import com.sumologic.shellbase.{ShellCommand, ShellCommandSet}
import org.apache.commons.cli.{CommandLine, Options}

class NotificationCommandSet(manager: ShellNotificationManager)
  extends ShellCommandSet("notifications", "Helps manage notifications for the shell") {

  override def shouldRunNotifications(arguments: List[String], commandPath: List[String] = List()) = false

  commands += new ShellCommand("list", "List available notification types and their status") {
    override def execute(cmdLine: CommandLine): Boolean = {
      val table = new ASCIITable[String]
      table.addColumn("Name", 20, s => s)
      table.addColumn("Enabled?", 10, manager.notificationEnabled(_).toString)

      table.renderLines(manager.notifierNames).foreach(println)

      true
    }
  }


  private class StatusShellCommand(command: String, desc: String, value: Boolean) extends ShellCommand(command, desc) {
    private val arg = new CommandLineArgument("notifications", 0, isRequired = false)

    override val maxNumberOfArguments = 1

    override def addOptions(options: Options): Unit = {
      super.addOptions(options)
      options += arg
    }

    override def execute(cmdLine: CommandLine): Boolean = {
      val input = cmdLine.get(arg).getOrElse("all")

      val cmd = if (value) {
        manager.enable _
      } else {
        manager.disable _
      }

      if (input == "all") {
        manager.notifierNames.foreach(cmd)
      } else {
        input.split(",").foreach(cmd)
      }

      true
    }
  }

  commands += new StatusShellCommand("enable", "Enable a notification", true)
  commands += new StatusShellCommand("disable", "Disable a notification", false)

}
