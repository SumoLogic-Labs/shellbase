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

import com.sumologic.shellbase.cmdline.CommandLineFlag
import jline.console.completer.AggregateCompleter
import org.apache.commons.cli.{CommandLine, Options}
import com.sumologic.shellbase.cmdline.RichCommandLine._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object ShellCommandSet {
  type ExecuteHook = (ShellCommand, Seq[String]) => Unit
}

class DuplicateCommandException(message: String) extends RuntimeException(message)
class InvalidCommandNameException(message: String) extends RuntimeException(message)

/**
  * A group of shell commands.
  */
class ShellCommandSet(name: String, helpText: String, aliases: List[String] = List[String]())
  extends ShellCommand(name, helpText, aliases) {

  val commands = ListBuffer[ShellCommand]()
  val preExecuteHooks = ListBuffer[ShellCommandSet.ExecuteHook]()
  val postExecuteHooks = ListBuffer[ShellCommandSet.ExecuteHook]()

  private val emptyCommandLine = parseOptions(Seq())

  private[this] def preExecute(shellCommand: ShellCommand, arguments: Seq[String]): Unit = {
    preExecuteHooks.foreach(_(shellCommand, arguments))
  }

  private[this] def postExecute(shellCommand: ShellCommand, arguments: Seq[String]): Unit = {
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

  override def shouldRunNotifications(args: List[String], commandPath: List[String]): Boolean =
    args match {
      case command :: arguments =>
        findCommand(command).exists(_.shouldRunNotifications(arguments, commandPath ++ List(command.trim)))
      case _ => false
    }

  // NOTE(stefan, 2013-12-31): Disable the validation done in ShellCommand, executeLine does
  // something comparable that makes more sense for this use case.
  override def validate(cmdLine: CommandLine): Option[String] = None

  def validateCommands(): Unit = {

    var seen = Map[String, String]()

    def checkCommandName(name: String, className: String): Unit = {
      if (seen.contains(name)) {
        val otherClass = seen(name)
        throw new DuplicateCommandException(s"Command '$name' is defined in $otherClass and $className!")
      } else {
        seen = seen + (name -> className)
      }
    }

    def checkShellCommand(command: ShellCommand): Unit = {
      val className = command.getClass.getName
      commandVariants(command).foreach(checkCommandName(_, className))
      command match {
        case set: ShellCommandSet => set.validateCommands()
        case _ =>
      }
    }

    commands.foreach(checkShellCommand)
  }

  def namingConvention(): CommandNamingConvention = new SeparatorNamingConvention("-", List("_", ""))

  def validateCommandNames(): Unit = {
    def checkNameConvention(name: String) {
      if (!namingConvention.validateName(name)) {
        throw new InvalidCommandNameException(s"Name $name does not match the convention")
      }
    }
    commands.foreach(command => {
      checkNameConvention(command.name)
      command match {
        case set: ShellCommandSet => set.validateCommandNames()
        case _ =>
      }
    })
  }

  final def execute(cmdLine: CommandLine) =
    throw new IllegalAccessException("Call the other signature!")

  override def argCompleter = new AggregateCompleter(commands.map(_.completer).toSeq: _*)

  def commandVariants(command: ShellCommand): List[String] = {
    command.basicVariants.flatMap(namingConvention.nameVersions(_)).distinct
  }

  def findCommand(input: String): Option[ShellCommand] = {
    val normalizedCommand = input.toLowerCase.trim
    commands.find(command => commandVariants(command).contains(normalizedCommand))
  }

  protected def printHelp(args: List[String], showAllCommands: Boolean): Boolean = {
    args match {
      case Nil =>
        printf("Available commands: %n")
        var filteredCommands = commands.filterNot(_.deprecated)
        var somethingWasHidden = false
        if (!showAllCommands) {
          somethingWasHidden = filteredCommands.exists(_.hiddenInHelp)
          filteredCommands = filteredCommands.filterNot(_.hiddenInHelp)
        }
        filteredCommands.sortBy(_.name).foreach(cmd => {
          printf("  %-15s %s%n", cmd.name, cmd.helpText)
        })
        if (somethingWasHidden) {
          println(s"Some commands were hidden. Use [-${ShowAllCommands.shortName}] flag to show them.")
        }
        true
      case command :: rest =>
        findCommand(command) match {
          case Some(shellCommandSet: ShellCommandSet) => shellCommandSet.printHelp(rest, showAllCommands)
          case Some(shellCommand) if rest.nonEmpty =>
            println(s"Command '$command' doesn't have subcommands")
            false
          case Some(shellCommand) =>
            shellCommand.help
            true
          case None =>
            println(s"Command '$command' unknown.")
            false
        }
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Commands available with all command sets.
  // -----------------------------------------------------------------------------------------------

  private val ShowAllCommands = new CommandLineFlag("a", "all", "Show all commands, including hidden")

  commands += new ShellCommand("help", "Print online help.", List("?")) {

    override def maxNumberOfArguments: Int = Int.MaxValue

    def execute(cmdLine: CommandLine): Boolean = {
      val showAllCommands = cmdLine.checkFlag(ShowAllCommands)
      printHelp(cmdLine.getArgs.toList.map(_.trim).filter(_.nonEmpty), showAllCommands)
    }

    override def addOptions(opts: Options): Unit = {
      super.addOptions(opts)
      opts += ShowAllCommands
    }
  }
}
