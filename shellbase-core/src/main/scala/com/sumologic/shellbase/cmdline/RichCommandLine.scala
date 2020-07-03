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
package com.sumologic.shellbase.cmdline

import java.io.File

import com.sumologic.shellbase.{ExitShellCommandException, ShellPrompter, ValidationFailure, ValidationSuccess}
import org.apache.commons.cli.{CommandLine, GnuParser, HelpFormatter, Options, ParseException}

/**
  * Wraps apache commons cli in a more scala-like fashion.
  */
object RichCommandLine {
  implicit def wrapCommandLine(cmdLine: CommandLine): RichCommandLine = new RichCommandLine(cmdLine)

  implicit def wrapOptions(options: Options): RichOptions = new RichOptions(options)

  implicit def wrapParser(args: Array[String]): RichCommandLineParser = new RichCommandLineParser(args)

  implicit def wrapOption[T](opt: Option[T]): RichScalaOption[T] = new RichScalaOption(opt)
}

class RichScalaOption[T](optn: Option[T]) {

  private[cmdline] def exit(message: String): Unit = throw new ExitShellCommandException(message)

  def getOrExitWithMessage(message: String): T = {
    optn match {
      case Some(value) =>
        value
      case None =>
        exit(message)
        null.asInstanceOf[T]
    }
  }
}

class RichCommandLineParser(args: Array[String]) {

  def parseCommandLine(options: Options, processName: String = "dsh"): Option[CommandLine] = {
    val parser = new GnuParser()
    try {
      Some(parser.parse(options, args))
    } catch {
      case e: ParseException => {
        println(e.getMessage)
        new HelpFormatter().printHelp(processName, options)
        None
      }
    }
  }
}

class RichCommandLine(cmdLine: CommandLine) {

  private lazy val prompter = new ShellPrompter()

  def get(elem: CommandLineElementWithValue, default: Option[String] = None): Option[String] = {
    val value = elem match {
      case option: CommandLineOption => getOption(option)
      case arg: CommandLineArgument => getArg(arg)
    }

    require(!elem.isRequired || value.isDefined, s"'${elem.name}' not set!")

    value match {
      case Some(string) => elem.validator(string) match {
        case Some(error) => throw new IllegalArgumentException(s"Error in command line: $error")
        case None => value
      }
      case None => default
    }
  }

  def apply(elem: CommandLineElementWithValue): String = get(elem).getOrElse {
    throw new NoSuchElementException(s"${elem.name} not set!")
  }

  private def getArg(arg: CommandLineArgument): Option[String] = {
    val args = cmdLine.getArgs
    if (args != null && args.length > arg.index) {
      Some(args(arg.index))
    } else {
      arg.defaultValue
    }
  }

  private def getOption(option: CommandLineOption): Option[String] = {
    cmdLine.hasOption(option.longName) match {
      case true => Some(cmdLine.getOptionValue(option.longName))
      case false => option.defaultValue
    }
  }

  // Don't append any punctuations to `question`, ": " is added automatically.
  def getOrPrompt(elt: CommandLineElementWithValue, question: Option[String] = None): String = {
    get(elt) match {
      case Some(res) => res
      case None =>
        val validator = elt.validator(_: String) match {
          case Some(error) => ValidationFailure(error)
          case None => ValidationSuccess
        }
        val actualQuestion = question.getOrElse(elt.name)
        prompter.askQuestion(actualQuestion, Seq(validator))
    }
  }

  def getList(element: CommandLineElementWithValue, separator: String = ",",
              default: Option[Seq[String]] = None): Option[Seq[String]] = {
    get(element) match {
      case Some(string) => Some(string.split(separator).map(_.trim))
      case None => default
    }
  }

  def getIntList(element: CommandLineElementWithValue, separator: String = ",",
                 default: Option[Seq[String]] = None): Option[Seq[Int]] = {
    getList(element, separator, default).map(_.map(_.toInt))
  }

  def getInt(element: CommandLineElementWithValue, default: Option[Int] = None): Option[Int] = {
    get(element) match {
      case Some(string) => Some(string.toInt)
      case None => default
    }
  }

  def getLong(element: CommandLineOption, default: Option[Long] = None): Option[Long] = {
    get(element) match {
      case Some(string) => Some(string.toLong)
      case None => default
    }
  }

  def getDouble(element: CommandLineOption, default: Option[Double] = None): Option[Double] = {
    get(element) match {
      case Some(string) => Some(string.toDouble)
      case None => default
    }
  }

  def checkFlag(option: CommandLineFlag): Boolean = {
    cmdLine.hasOption(option.longName)
  }

  def getBoolean(element: CommandLineElementWithValue, default: Option[Boolean]): Option[Boolean] = {
    get(element) match {
      case Some(string) => Some(string.toBoolean)
      case None => default
    }
  }

  def getFile(element: CommandLineElementWithValue,
              checkExistence: Boolean = true,
              default: Option[File]): Option[File] = {
    val path = get(element) match {
      case Some(string) => Some(new File(string))
      case None => default
    }

    path match {
      case Some(file) => {
        if (!checkExistence || (file.exists() && file.isFile)) {
          Some(file)
        } else {
          None
        }
      }

      case None => None
    }
  }

  def getDirectory(element: CommandLineElementWithValue, checkExistence: Boolean = true,
                   default: Option[File]): Option[File] = {
    val path = get(element) match {
      case Some(string) => Some(new File(string))
      case None => default
    }

    path match {
      case Some(directory) => {
        if (!checkExistence || (directory.exists() && directory.isDirectory)) {
          Some(directory)
        } else {
          None
        }
      }

      case None => None
    }
  }
}

class RichOptions(options: Options) {

  var arguments = List[CommandLineArgument]()

  def +=(arg: CommandLineElement): Unit = {
    require(arg != null)
    arg match {
      case a: CommandLineArgument => this addArgument a
      case f: CommandLineFlag => this addFlag f
      case o: CommandLineOption => this addOption o
      case _ => throw new IllegalArgumentException("Unknown type")
    }
  }

  def addAll(args: CommandLineElement*): Unit = {
    args foreach {
      this.+=
    }
  }

  def addArgument(arg: CommandLineArgument): Unit = {
    require(arg != null)

    if (arguments.exists(_.index == arg.index)) {
      throw new IllegalArgumentException("Argument with index %d already defined!".format(arg.index))
    }

    options match {
      case opt: ArgumentTrackingOptions => opt.addArgument(arg)
      case _ => {}
    }

    arguments +:= arg
  }

  def addFlag(flag: CommandLineFlag): Unit = {

    require(flag != null)

    import flag._
    options.addOption(shortName, longName, false, helpText)
  }

  def addOption(option: CommandLineOption): Unit = {

    require(option != null)

    import option._

    List(shortName, longName).foreach {
      name => require(options.getOption(name) == null, "Option %s already defined!".format(name))
    }

    val helpTextWithDefault = defaultValue match {
      case Some(default) => "%s (default: %s)".format(helpText, default)
      case None => helpText
    }

    options.addOption(shortName, longName, true, helpTextWithDefault)
  }
}

object CommandLineValidators {
  def noop(value: String) = None

  def validInteger(value: String) = try {
    value.toInt
    None
  } catch {
    case nfe: NumberFormatException => Some(f"$value is not a valid integer")
  }

  def validPositiveInteger(value: String) = {
    try {
      validInteger(value)
      require(value.toInt > 0)
      None
    }
    catch {
      case e: Exception => Some(s"$value is not a valid positive integer")
    }
  }

  def validNonNegativeInteger(value: String) = {
    try {
      validInteger(value)
      require(value.toInt >= 0)
      None
    }
    catch {
      case e: Exception => Some(s"$value is not a valid non-negative integer")
    }
  }
}

trait CommandLineElement {
  def name: String
}

class CommandLineElementWithValue(val name: String,
                                  val isRequired: Boolean,
                                  val validator: String => Option[String] = CommandLineValidators.noop)
  extends CommandLineElement

/**
  * An optional input to the command, can be specified via short name or long name and is always
  * followed by a value argument, i.e. -a infrastructure or --account infrastructure.
  */
class CommandLineOption(val shortName: String,
                        val longName: String,
                        isRequired: Boolean,
                        val helpText: String,
                        val defaultValue: Option[String] = None,
                        validator: String => Option[String] = CommandLineValidators.noop)
  extends CommandLineElementWithValue(longName, isRequired, validator)

/**
  * a simple boolean, often used to enable or disable some of the features of a command.
  */
class CommandLineFlag(val shortName: String,
                      val longName: String,
                      val helpText: String)
  extends CommandLineElement {
  override def name: String = longName
}

/**
  * A plain argument specified on the command line, without a leading dash.
  * Conventionally, they are the primary input to a command, often a mandatory one.
  */
class CommandLineArgument(name: String,
                          val index: Int,
                          isRequired: Boolean,
                          val defaultValue: Option[String] = None,
                          validator: String => Option[String] = CommandLineValidators.noop)
  extends CommandLineElementWithValue(name, isRequired, validator)


