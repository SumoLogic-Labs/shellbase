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

import com.sumologic.shellbase.cmdline.ArgumentTrackingOptions

import jline.console.completer.{Completer, NullCompleter, StringsCompleter}
import org.apache.commons.cli.{CommandLine, GnuParser, HelpFormatter, Options, ParseException, UnrecognizedOptionException}
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

class ExitShellCommandException(message: String) extends RuntimeException(message)

/**
  * Definition of a shell command.
  */
abstract class ShellCommand(val name: String,
                            val helpText: String,
                            val aliases: List[String] = List[String](),
                            val deprecated: Boolean = false,
                            val hiddenInHelp: Boolean = false,
                            val usageText: Option[String] = None)
                           (implicit e: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global) {

  protected val _logger = LoggerFactory.getLogger(getClass)
  protected lazy val prompter = new ShellPrompter()

  private val _currentCommand = new ThreadLocal[String]()

  protected def currentCommand: String = _currentCommand.get()

  def executeLine(arguments: List[String], commandPath: List[String] = List()): Boolean = {
    _currentCommand.set {
      val cmd = commandPath.mkString(" ").trim
      val args = arguments.mkString(" ").trim
      if (args.nonEmpty) {
        s"$cmd $args"
      } else {
        s"$cmd"
      }
    }
    var commandResult: Boolean = false
    var slackTsOpt: Option[String] = None
    val timeNow = System.currentTimeMillis()

    try {
      val cmdLine = parseOptions(arguments)

      validate(cmdLine) match {
        case Some(error) => {
          println(error)
          false
        }
        case None => {
          // do NOT block the command from executing
          Future {
            slackTsOpt = postCommandToSlack(commandPath, arguments)
          }

          commandResult = execute(cmdLine)

          commandResult
        }
      }
    } catch {
      case e: IllegalArgumentException => {
        println(ShellColors.red(e.getMessage))
        _logger.debug("", e)
        false
      }
      case e: ExitShellCommandException => {
        println(ShellColors.red(e.getMessage))
        _logger.debug("", e)
        false
      }
      case e: UnrecognizedOptionException => {
        println(ShellColors.red(e.getMessage))
        println("To set negative numbers as property value then quote and prefixing it with a space. For ex. \" -1\"")
        help
        false
      }
      case e: ParseException => {
        println(ShellColors.red(e.getMessage))
        help
        false
      }
      case e: Throwable => {
        _logger.debug("", e)
        e.printStackTrace()
        false
      }
    } finally {
      Future{
        // we calculate how long it takes from starting the command to finishing the command
        val commandExecuteTimeDuration = (System.currentTimeMillis() - timeNow) / 60000
        slackTsOpt.foreach { ts =>
          postInformationToSlackThread(ts, commandExecuteTimeDuration, commandResult)
        }
      }

      _currentCommand.remove()
    }
  }

  /**
    * Execute the command. The command line passed in contains everything the user specified - options
    * and other arguments. Returns true if the command succeeded, false otherwise.
    */
  def execute(cmdLine: CommandLine): Boolean

  final def parseOptions(arguments: Seq[String]): CommandLine = {
    val options = new Options()
    addOptions(options)
    new GnuParser().parse(options, arguments.toArray)
  }

  def validate(cmdLine: CommandLine): Option[String] = {
    if (maxNumberOfArguments >= 0 && cmdLine.getArgs.size > maxNumberOfArguments) {
      if (maxNumberOfArguments == 0) {
        Some("Unexpected command line arguments: %s".format(cmdLine.getArgs.mkString(" ")))
      } else {
        Some("At most %d arguments expected, but %d received.".format(maxNumberOfArguments, cmdLine.getArgs.size))
      }
    } else {
      None
    }
  }

  def postCommandToSlack(cmdPath: List[String], args: List[String]): Option[String] = {
    None
  }

  def postInformationToSlackThread(ts: String,
                                   commandExecuteTimeDuration: Long,
                                   commandResult: Boolean): Option[String] = {
    None
  }

  // NOTE(stefan, 2012-03-01): -1 for unlimited.
  def maxNumberOfArguments = 0

  def addOptions(opts: Options) = {}

  /**
    * Returns a completer that jline can use for tab completion.
    */
  def argCompleter: Completer = new NullCompleter

  /**
    * Returns `true` iff notifications should be run for this command
    */
  def shouldRunNotifications(arguments: List[String], commandPath: List[String] = List()): Boolean = true

  // -----------------------------------------------------------------------------------------------
  // Internals.
  // -----------------------------------------------------------------------------------------------

  final def help = {
    val options = new ArgumentTrackingOptions
    addOptions(options)

    val arguments = options.getArguments.sortBy(_.index).map {
      opt => {
        if (opt.isRequired) {
          opt.name
        } else {
          s"[${opt.name}]"
        }
      }
    }

    val txt = s"$name ${arguments.mkString(" ")}"

    val fullHelpText: String = usageText match {
      case Some(text) => helpText + "\n\n" + text
      case _ => helpText
    }

    new HelpFormatter().printHelp(HelpFormatter.DEFAULT_WIDTH, txt, fullHelpText, options,
      null, // footer
      true) // print an automatically generated usage statement, instead of:
      //  usage: sleep period
      // It prints:
      //  usage: sleep period [-v]
  }

  def basicVariants: List[String] = List(name) ++ aliases

  final def completer = {
    new NestedCompleter(new StringsCompleter(basicVariants), argCompleter)
  }
}
