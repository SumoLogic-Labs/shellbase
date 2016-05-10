package com.sumologic.shellbase.notifications

import com.sumologic.shellbase.cmdline.RichCommandLine._
import com.sumologic.shellbase.cmdline.{CommandLineArgument, RichCommandLine}
import com.sumologic.shellbase.table.ASCIITable
import com.sumologic.shellbase.{ShellCommand, ShellCommandSet}
import org.apache.commons.cli.{CommandLine, Options}

class NotificationCommandSet(manager: ShellNotificationManager)
  extends ShellCommandSet("notifications", "Helps manage notifications for the shell") {

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
