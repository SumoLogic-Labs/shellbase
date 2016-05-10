package com.sumologic.shellbase

import com.sumologic.shellbase.cmdline.ArgumentTrackingOptions
import jline.console.completer.{Completer, NullCompleter, StringsCompleter}
import org.apache.commons.cli.{CommandLine, GnuParser, HelpFormatter, Options, ParseException, UnrecognizedOptionException}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
  * Definition of a shell command.
  */
abstract class ShellCommand(val name: String,
                            val helpText: String,
                            val aliases: List[String] = List[String](),
                            val deprecated: Boolean = false) {

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
    try {
      val cmdLine = parseOptions(arguments)

      validate(cmdLine) match {
        case Some(error) => {
          println(error)
          false
        }
        case None => {
          // do NOT block the command from executing
          import scala.concurrent.ExecutionContext.Implicits.global
          Future {
            postCommandToSlack(commandPath, arguments)
          }

          execute(cmdLine)
        }
      }
    } catch {
      case e: IllegalArgumentException => {
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

  // NOTE(stefan, 2012-03-01): -1 for unlimited.
  def maxNumberOfArguments = 0

  def addOptions(opts: Options) = {}

  /**
    * Returns a completer that jline can use for tab completion.
    */
  def argCompleter: Completer = new NullCompleter

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

    new HelpFormatter().printHelp(txt, options)
  }

  final def completer = {
    val variants = List(name) ++ aliases
    new NestedCompleter(new StringsCompleter(variants), argCompleter)
  }
}
