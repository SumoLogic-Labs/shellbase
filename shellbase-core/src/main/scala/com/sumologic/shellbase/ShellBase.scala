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

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import com.sumologic.shellbase.commands._
import com.sumologic.shellbase.interrupts.{InterruptKeyMonitor, KillableSingleThread}
import com.sumologic.shellbase.notifications._
import com.sumologic.shellbase.timeutil.TimeFormats
import jline.console.{ConsoleReader, UserInterruptException}
import jline.console.history.FileHistory
import org.apache.commons.cli.{CommandLine, GnuParser, HelpFormatter, Options, ParseException, Option => CLIOption}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.util.Success

/**
  * A shell base that can be used to build shell like applications.
  */
abstract class ShellBase(val name: String)
{

  // -----------------------------------------------------------------------------------------------
  // Abstract methods to implement.
  // -----------------------------------------------------------------------------------------------

  /**
    * Return the list of commands.
    */
  def commands: Seq[ShellCommand] = List[ShellCommand]()

  /**
    * Return additional command line options.
    */
  def additionalOptions: Seq[CLIOption] = List[CLIOption]()

  /**
    * Process additional command line options.
    */
  def init(cmdLine: CommandLine): Boolean = true

  /**
    * Override for a custom prompt.
    */
  def prompt: String = "$"

  /**
    * Return a custom banner.
    */
  def banner: String = ""

  /**
    * User personal directory with shell-related stuff.
    */
  def personalDir: File = new File(new File(System.getProperty("user.home")), "." + name)

  /**
    * Where to look for scripts.
    */
  def scriptDir: File = new File("scripts/")

  /**
    * Where to look for personal scripts.
    */
  def personalScriptDir: File = new File(personalDir, "scripts/")

  /**
    * Start-up script
    */
  def initScriptOpt: Option[File] =
    Option(new File(personalScriptDir, s".init.$scriptExtension")).filter(f => f.exists && f.canRead)

  /**
    * File extension for scripts (without leading dot).
    */
  def scriptExtension: String = name

  /**
    * Prints commands to stdout as they're executed
    */
  def verboseMode: Boolean = false

  /**
    * Name of the history file.
    */
  // NOTE(konstantin, 2017-27-02): This should probably be moved to $personalDir
  def historyPath: File = new File("%s/.%s_history".format(System.getProperty("user.home"), name))

  /**
    * Hide in help built-in commands.
    */
  def hideBuiltInCommandsFromHelp(commandName: String): Boolean = false

  /**
    * Manages notifications
    */
  lazy val notificationManager: ShellNotificationManager = new InMemoryShellNotificationManager(
    name, Seq(new RingingNotification, new PopupNotification)
  )

  /**
    * Pre, post command hooks
    */
  protected[this] def preCommandHooks: Seq[ShellCommandSet.ExecuteHook] = List()

  protected[this] def postCommandHooks: Seq[ShellCommandSet.ExecuteHook] = List()

  private val _logger = LoggerFactory.getLogger(getClass)

  /**
    * Exit the shell.
    */
  protected def exitShell(exitValue: Int) {
    System.exit(exitValue)
  }

  // rootSet is visible for testing only
  private[shellbase] val rootSet = new ShellCommandSet(name, "")
  rootSet.preExecuteHooks.appendAll(preCommandHooks)
  rootSet.postExecuteHooks.appendAll(postCommandHooks)

  private val history = new FileHistory(historyPath)
  history.setMaxSize(1000)

  // NOTE(stefan, 2014-01-06): From the jline JavaDoc:Implementers should install shutdown hook to
  // call {@link FileHistory#flush} to save history to disk.
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = {
      history.flush()
      interruptKeyMonitor.shutdown()
    }
  })

  // VisibleForTesting
  private[shellbase] val interruptKeyMonitor = new InterruptKeyMonitor()
  interruptKeyMonitor.init()

  private val reader = new ConsoleReader()
  reader.setHistory(history)
  reader.setHandleUserInterrupt(true)

  def main(args: Array[String]) = {
    Thread.currentThread.setName("Shell main")
    try {
      val result = actualMain(args)
      if (result != 0) {
        exitShell(result)
      } else {
        // let the program end naturally
      }
    } finally {
      interruptKeyMonitor.shutdown()
    }
  }

  def actualMain(args: Array[String]): Int = {

    val options = new Options
    for (optn <- additionalOptions) {
      options.addOption(optn)
    }

    options.addOption(null, "no-exit", false,
      "Don't exit after executing the command passed on the command line.")

    val cmdLine: CommandLine = try {
      new GnuParser().parse(options, args, true)
    } catch {
      case e: ParseException =>
        println(e.getMessage)
        new HelpFormatter().printHelp(name, options)
        return 1
    }

    def runInitScript(): Unit = {
      for (initScript <- initScriptOpt) {
        runScriptCommand.executeLine(List(initScript.getAbsolutePath))
      }
    }

    if (init(cmdLine)) {
      initializeCommands()

      val arguments = cmdLine.getArgs
      if (arguments.nonEmpty) {
        val interactiveAfterScript = cmdLine.hasOption("no-exit")

        if (interactiveAfterScript) {
          reader.clearScreen
          println(banner)
        }

        val scriptSucceeded = rootSet.executeLine(List[String]() ++ arguments)

        if (!interactiveAfterScript && !scriptSucceeded) {
          println(s"Execution failed! Input was ${arguments.mkString(" ")}")
          return 1
        }

        if (interactiveAfterScript) {
          runInitScript()
          interactiveMainLoop()
        }
      } else {
        reader.clearScreen

        println(banner)

        runInitScript()
        interactiveMainLoop()
      }

      0
    } else {
      println("Could not initialize!")
      1
    }
  }

  final def initializeCommands() {
    val customCommands = commands
    rootSet.commands ++= customCommands
    validateCommands()
  }

  def validateCommands() = {
    rootSet.validateCommands()
    if (enforceNamingConventions) {
      rootSet.validateCommandNames()
    }
  }

  private def interactiveMainLoop() {
    println("  Enter your commands below. Type 'help' for help. ")

    reader.setAutoprintThreshold(128) // allow more completions candidates without prompting
    reader.addCompleter(rootSet.argCompleter)

    var keepRunning = true
    while (keepRunning) {
      val line = try {
        reader.readLine(prompt)
      } catch {
        case _: UserInterruptException => "" // do nothing, that's fine
      }

      // Line is null on CTRL-D...
      if (line == null) {
        println()
        keepRunning = false
      } else {
        runKillableCommand(line)
      }
    }

    println("Exiting...")
  }

  // VisibleForTesting
  private[shellbase] def runKillableCommand(line: String): Boolean = {
    val commandRunner = new KillableSingleThread(runCommand(line))

    // Wait a bit for completion, so quick commands don't need to go through the keyMonitor.
    val startTime = now
    commandRunner.start()
    commandRunner.waitForCompletion(Duration(200, TimeUnit.MILLISECONDS))

    // Start the keyMonitor to watch for key interrupts.
    if (!commandRunner.future.isCompleted) {
      interruptKeyMonitor.startMonitoring(interruptCallback = commandRunner.synchronized {
        if (!commandRunner.future.isCompleted) {
          println(s"Caught interrupt.")
          println(s"Killing command with 1s grace period: `$line`...")
          commandRunner.kill(Duration(1, TimeUnit.SECONDS))
        }
      })

      commandRunner.waitForCompletion(Duration.Inf)
      val runTimeInSec = (now - startTime) / 1000
      val timing = TimeFormats.formatAsTersePeriod(now - startTime)
      _logger.debug(s"Done running command `$line` (took: ${runTimeInSec}s [$timing]).")

      interruptKeyMonitor.stopMonitoring()
    }

    commandRunner.future.value match {
      case Some(Success(result)) => result
      case _ => false
    }
  }

  def runCommand(line: String): Boolean = {
    val tokens = parseLine(line)
    val tokensIterator = tokens.iterator
    while (tokensIterator.hasNext) {
      val newTokens = tokensIterator.takeWhile(_.trim != "&&").toList
      _logger.debug(s"Executing: $newTokens")
      if (verboseMode) {
        println(s"Executing: $newTokens")
      }
      val exitStatus = runSingleTokenizedCommand(newTokens)
      if (!exitStatus) {
        if (tokensIterator.hasNext) {
          println(s"Execution failed for `${newTokens.mkString(" ")}`.")
          println(s"Skipping the remaining commands: `${tokensIterator.toList.mkString(" ")}`")
        }
        return false
      }
    }
    true
  }

  private def runSingleTokenizedCommand(tokens: List[String]): Boolean = {
    val out = rootSet.executeLine(tokens)

    if (rootSet.shouldRunNotifications(tokens)) {
      val cmd = tokens.mkString("'", " ", "'")
      val msg = if (out) {
        s"Command finished successfully: $cmd"
      } else {
        s"Command failed: $cmd"
      }

      notificationManager.notify(msg)
    }

    out
  }

  private def now = System.currentTimeMillis()

  protected def parseLine(line: String): List[String] = ShellBase.parseLine(line)

  // -----------------------------------------------------------------------------------------------
  // Little helpers.
  // -----------------------------------------------------------------------------------------------

  def getListParameter(cmdLine: CommandLine, name: String): Seq[String] = {
    val str = cmdLine.getOptionValue(name)
    if (str == null) {
      return List[String]()
    }

    str.split(",")
  }

  // -----------------------------------------------------------------------------------------------
  // Root commands.
  // -----------------------------------------------------------------------------------------------

  val subCommandExtractor = ShellBase.SubCommandExtractor

  rootSet.commands += new ClearCommand {
    override val hiddenInHelp = hideBuiltInCommandsFromHelp(name)
  }

  rootSet.commands += new ExitCommand(exitShell)  {
    override val hiddenInHelp = hideBuiltInCommandsFromHelp(name)
  }

  rootSet.commands += new SleepCommand  {
    override val hiddenInHelp = hideBuiltInCommandsFromHelp(name)
  }

  rootSet.commands += new EchoCommand  {
    override val hiddenInHelp = hideBuiltInCommandsFromHelp(name)
  }

  rootSet.commands += new TeeCommand(runCommand)  {
    override val hiddenInHelp = hideBuiltInCommandsFromHelp(name)
  }

  rootSet.commands += new TimeCommand(runCommand)  {
    override val hiddenInHelp = hideBuiltInCommandsFromHelp(name)
  }

  private val runScriptCommand = new RunScriptCommand(
    List(scriptDir, personalScriptDir, new File(System.getProperty("user.dir"))),
    scriptExtension, runCommand, parseLine
  ) {
    override val hiddenInHelp = hideBuiltInCommandsFromHelp(name)
  }
  rootSet.commands += runScriptCommand

  rootSet.commands += new NotificationCommandSet(notificationManager) {
    // NOTE(chris, 2014-02-05): This has to be near the end for overrides to work
    override val hiddenInHelp = hideBuiltInCommandsFromHelp(name)
  }

  def enforceNamingConventions: Boolean = false
}

object ShellBase {
  val SubCommandExtractor = "`([^`]*)`".r // Regex extractor for commands within `-quotes.

  def parseLine(line: String): List[String] = {
    if (line.trim.startsWith("#") || line.trim.length < 1) {
      return List[String]()
    }

    var parsedLine = List[String]()
    val regex = Pattern.compile("[^\\s\"'`]+|\"([^\"]*)\"|'([^']*)'|`([^`]*)`")
    val regexMatcher = regex.matcher(line)
    while (regexMatcher.find()) {
      if (regexMatcher.group(1) != null) {
        // Add double-quoted string without the quotes
        parsedLine :+= regexMatcher.group(1)
      } else if (regexMatcher.group(2) != null) {
        // Add single-quoted string without the quotes
        parsedLine :+= regexMatcher.group(2)
      } else if (regexMatcher.group(3) != null) {
        // Add `-quoted string with the quotes
        parsedLine :+= "`" + regexMatcher.group(3) + "`"
      } else {
        // Add unquoted word
        parsedLine :+= regexMatcher.group()
      }
    }

    parsedLine
  }
}
