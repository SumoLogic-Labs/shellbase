package com.sumologic.shellbase

import jline.console.completer.AggregateCompleter
import org.apache.commons.cli.CommandLine

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object ShellCommandSet {
  type ExecuteHook = (ShellCommand, Seq[String]) => Unit
}

class DuplicateCommandException(message: String) extends RuntimeException(message)

/**
  * A group of shell commands.
  */
class ShellCommandSet(name: String, helpText: String, aliases: List[String] = List[String]())
  extends ShellCommand(name, helpText, aliases) {

  val commands = ListBuffer[ShellCommand]()
  val preExecuteHooks = ListBuffer[ShellCommandSet.ExecuteHook]()
  val postExecuteHooks = ListBuffer[ShellCommandSet.ExecuteHook]()

  private val emptyCommandLine = parseOptions(Seq())

  private[this] def preExecute(shellCommand: ShellCommand, arguments: Seq[String]) {
    preExecuteHooks.foreach(_(shellCommand, arguments))
  }

  private[this] def postExecute(shellCommand: ShellCommand, arguments: Seq[String]) {
    postExecuteHooks.foreach(_(shellCommand, arguments))
  }

  final override def executeLine(args: List[String], commandPath: List[String] = List()): Boolean = {

    if (args.length < 1) {
      println("Please run 'help' to learn more about this command set.")
      return true
    }

    val command = args(0)
    val arguments = args.splitAt(1)._2
    findCommand(command) match {

      case Some(shellCommand) =>
        validate(emptyCommandLine).foreach {
          error =>
            println(error)
            return false
        }

        try {
          preExecute(shellCommand, arguments)
        } catch {
          case e: Exception =>
            println(e.getMessage)
            return false
        }

        val ret = shellCommand.executeLine(arguments, commandPath ++ List(command.trim))

        try {
          postExecute(shellCommand, arguments)
        } catch {
          case e: Exception =>
            println(e.getMessage)
            return false
        }

        ret

      case None =>
        println(s"$name: command $command not found")
        false
    }
  }

  // NOTE(stefan, 2013-12-31): Disable the validation done in ShellCommand, executeLine does
  // something comparable that makes more sense for this use case.
  override def validate(cmdLine: CommandLine): Option[String] = None

  def validateCommands(): Unit = {

    var seen = Map[String, String]()

    def checkCommandName(name: String, className: String) {
      if (seen.containsKey(name)) {
        val otherClass = seen(name)
        throw new DuplicateCommandException(s"Command '$name' is defined in $otherClass and $className!")
      } else {
        seen = seen + (name -> className)
      }
    }

    def checkShellCommand(command: ShellCommand) {
      val className = command.getClass.getName
      checkCommandName(command.name, className)
      command.aliases.foreach(checkCommandName(_, className))
      command match {
        case set: ShellCommandSet => set.validateCommands()
        case _ =>
      }
    }

    commands.foreach(checkShellCommand)
  }

  final def execute(cmdLine: CommandLine) =
    throw new IllegalAccessException("Call the other signature!")

  override def argCompleter = new AggregateCompleter(commands.map(_.completer))

  def findCommand(command: String) = {
    val normalizedCommand = command.toLowerCase.trim
    commands.find(cmd => {
      cmd.name == normalizedCommand || cmd.aliases.contains(normalizedCommand)
    })
  }

  // -----------------------------------------------------------------------------------------------
  // Commands available with all command sets.
  // -----------------------------------------------------------------------------------------------

  commands += new ShellCommand("help", "Print online help.", List("?")) {

    override def maxNumberOfArguments = 1

    def execute(cmdLine: CommandLine) = {

      if (cmdLine.getArgList.size < 1 || cmdLine.getArgs()(0).trim.isEmpty) {
        printf("Available commands: %n")
        for (cmd <- commands.filterNot(_.deprecated).sortBy(_.name)) {
          printf("  %-15s %s%n", cmd.name, cmd.helpText)
        }
        true
      } else {
        val command = cmdLine.getArgs()(0)
        findCommand(command) match {
          case Some(shellCommand) =>
            shellCommand.help
            true
          case None =>
            printf("Command '%s' unknown.", command)
            false
        }
      }
    }
  }
}
