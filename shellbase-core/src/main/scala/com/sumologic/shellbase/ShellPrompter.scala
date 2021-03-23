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
import java.net.URL
import java.text.SimpleDateFormat
import java.util.{Date, GregorianCalendar}

import com.sumologic.shellbase.timeutil.TimeFormats
import jline.console.{ConsoleReader, UserInterruptException}

/**
  * Collection of ways to solicit validated input from a user on a command line.
  */
class ShellPrompter(in: ConsoleReader = new ConsoleReader) {

  // NOTE(carlton, 2014-01-08): There's probably more stuff we'd like to turn off here;
  // I wish ConsoleReader had a way to say "read a line, disabling all non-interrupt
  // characters" but I don't see it.  At any rate, with this it's safe to have a
  // password with a ! in it.
  in.setExpandEvents(false)

  private[shellbase] val asciiCR = 13.toChar

  /**
    * Asks user to confirm something.
    *
    * @param question The question to display (without trailing colon).
    * @return true if the user responded with yes.
    */
  def confirm(question: String): Boolean = {

    printf("%s [Y/N]: ", question)
    val response = in.readCharacter("ynYN".toArray: _*)
    println(response.toChar)

    response == 'y' || response == 'Y'
  }

  def confirmWithDefault(question: String, default: Boolean) = {
    val yesNo = if (default) "[Y/n]" else "[y/N]"
    print(s"$question $yesNo: ")
    val input = in.readCharacter(s"ynYN$asciiCR".toArray: _*)
    println(input.toChar)

    if (input == asciiCR) {
      default
    } else {
      input == 'y' || input == 'Y'
    }
  }

  def readChar(question: String, charsAllowed: Seq[Char]): Int = {
    printf(s"$question : ")
    if (charsAllowed.isEmpty) {
      in.readCharacter()
    } else {
      in.readCharacter(charsAllowed: _*)
    }
  }

  def confirmWithWarning(question: String): Boolean = {
    println(ShellBanner.Warning)
    confirm(question)
  }

  def askForTime(question: String, dateFormat: String = "d MMM HH:mm"): Date = {
    printf(question)
    val formatter = new SimpleDateFormat(dateFormat)
    val calendar = new GregorianCalendar()
    val prompt = s"Please enter date in following format - $dateFormat. Example - ${formatter.format(calendar.getTime)}. : "
    val userDate = in.readLine(prompt)
    val out = formatter.parse(userDate)
    // FIXME(carlton, 2015-08-10): If this were the last warning in util, then I'd figure
    // out what magic to put in here to get the right result, but for now I'm not
    // up with dealing with the Java Date/Calendar mess.
    if (out.getYear == 70) {
      // 70 = 1970
      out.setYear(new Date().getYear)
    }
    out
  }

  def askPasswordWithConfirmation(firstPrompt: String = "Password",
                                  secondPrompt: String = "Confirm",
                                  minLength: Int = 0,
                                  maxAttempts: Int = 3): String = {

    var attempts = 0
    while (attempts < maxAttempts) {
      val validators = List(ShellPromptValidators.nonEmpty _,
        ShellPromptValidators.lengthBetween(minLength, -1) _)
      val promptLength = math.max(firstPrompt.length, secondPrompt.length)
      val formatString = "%" + promptLength + "s"
      val password = askQuestion(formatString.format(firstPrompt), validators, maskCharacter = Some('*'))
      val confirm = askQuestion(formatString.format(secondPrompt), validators, maskCharacter = Some('*'))
      if (password == confirm) {
        return password
      }

      println(s"$firstPrompt does not match.")
      attempts += 1
    }

    null
  }

  def askQuestion(question: String,
                  validators: Seq[String => ValidationResult] = List[String => ValidationResult](),
                  maskCharacter: Option[Character] = None,
                  default: Option[String] = None,
                  maxAttempts: Int = 3): String = {

    val prompt = default match {
      case Some(default) => s"$question[$default]: "
      case None => s"$question: "
    }

    var attempts = 0
    var result: String = null
    while (result == null && attempts < maxAttempts) {
      result = in.readLine(prompt, maskCharacter.orNull).trim

      if ((result == null || result.trim.length < 1) && default.nonEmpty) {
        result = default.orNull
      }

      var valid = true
      for (validator <- validators) {
        val validationResult = validator(result)
        if (!validationResult.valid) {
          println(validationResult.message)
          valid = false
        }
      }

      if (!valid) {
        result = null
      }

      attempts += 1
    }

    require(result != null, s"No valid answer given after $maxAttempts attempts!")
    result
  }

  private def printOptions(options: Seq[String], allowNoSelection: Boolean): Unit = {
    var i = 1
    options.foreach(option => {
      printf("%3d) %s%n", i, option)
      i += 1
    })

    if (allowNoSelection) {
      printf("  0) None%n")
    }
  }

  def pickFromOptions(headline: String,
                      options: Seq[String],
                      default: String = null,
                      allowNoSelection: Boolean = false,
                      maxAttempts: Int = 3): String = {
    var defaultNumber: String = null
    if (default == null) {
      println(headline)
    } else {
      println(s"$headline[$default]")
      val index = options.indexOf(default)
      if (index > 0) {
        defaultNumber = (index + 1).toString
      }
    }

    val from = allowNoSelection match {
      case true => 0
      case false => 1
    }

    // Partially, partially applied :)
    import ShellPromptValidators._
    val validator = or(inNumberRange(from, options.length) _, inList(options) _) _

    var attempts = 0
    while (attempts < maxAttempts) {
      printOptions(options, allowNoSelection)
      val number = askQuestion("Selection", List(validator), default = Option(defaultNumber))
      if (number != null) {
        val no = Integer.parseInt(number)
        if (no == 0) {
          return null
        } else {
          return options(no - 1)
        }
      }

      attempts += 1
    }

    null
  }

  def pickMultipleFromVerboseOptions(headline: String,
                                     show_options: Seq[String],
                                     return_options: Seq[String],
                                     maxAttempts: Int = 3,
                                     allowNoSelection: Boolean = false): Seq[String] = {
    println(headline)

    val from = allowNoSelection match {
      case true => 0
      case false => 1
    }

    // Partially, partially applied :)
    import ShellPromptValidators._
    val validator = listOf(or(inNumberRange(from, return_options.length) _, inList(return_options) _) _) _

    var attempts = 0
    while (attempts < maxAttempts) {
      printOptions(show_options, allowNoSelection)
      val answer = askQuestion("Selection, you can list more than one by using commas", List(validator))
      if (answer != null) {

        val answers = answer.split(",")

        if (allowNoSelection && answers.contains("0")) {
          return Seq[String]()
        }

        return answers.map(_.trim).map(c => {
          try {
            return_options(Integer.parseInt(c) - 1)
          } catch {
            case _: NumberFormatException => {
              c
            }
          }
        })
      }

      attempts += 1
    }

    throw new Exception("Selection hasn't happened properly")
  }

  def pickMultipleFromOptions(headline: String,
                              options: Seq[String],
                              maxAttempts: Int = 3,
                              allowNoSelection: Boolean = false): Seq[String] = {
    pickMultipleFromVerboseOptions(headline, options, options, maxAttempts, allowNoSelection)
  }
}

abstract class ValidationResult(val valid: Boolean, val message: String)

object ValidationSuccess extends ValidationResult(true, null)

case class ValidationFailure(error: String) extends ValidationResult(false, error)

object ShellPromptValidators {

  def matchesRegex(regex: String, toUpper: Boolean = false)(result: String): ValidationResult = {

    val checkString = if (toUpper) result.toUpperCase else result

    if (checkString.matches(regex)) {
      ValidationSuccess
    } else {
      new ValidationFailure(s"Did not match regex $regex!")
    }
  }

  def empty(result: String): ValidationResult = {
    if (result.trim.length > 0) {
      new ValidationFailure("Please don't enter a value!")
    } else {
      ValidationSuccess
    }
  }

  def nonEmpty(result: String): ValidationResult = {
    if (result.trim.length > 0) {
      ValidationSuccess
    } else {
      new ValidationFailure("Please enter a value!")
    }
  }

  def numeric(result: String): ValidationResult = {
    try {
      Integer.parseInt(result)
      ValidationSuccess
    }
    catch {
      case e: NumberFormatException => {
        new ValidationFailure("Please enter a numeric value!")
      }
    }
  }

  def isFloatingNumber(result: String): ValidationResult = {
    try {
      result.toDouble
      return ValidationSuccess
    }
    catch {
      case e: NumberFormatException => {
        new ValidationFailure("Please enter a floating number!")
      }
    }

    ValidationFailure("Value must be a floating number!")
  }

  def inNumberRange(from: Int, to: Int)(result: String): ValidationResult = {
    if (numeric(result).valid) {
      val value = Integer.parseInt(result)
      if (value >= from && value <= to) {
        return ValidationSuccess
      }
    }

    ValidationFailure(s"Value must be between $from and $to.")
  }

  def inTimeRange(from: Long, to: Long)(result: String): ValidationResult = {
    val value = TimeFormats.parseTersePeriod(result)
    if (value.isEmpty) {
      return new ValidationFailure(s"Unable to parse value '$result' as time period")
    }
    if (value.get >= from && value.get <= to) {
      return ValidationSuccess
    }

    ValidationFailure(s"Value must be between ${TimeFormats.formatAsTersePeriod(from)} and ${TimeFormats.formatAsTersePeriod(to)}")
  }

  def inNumberDoubleRange(from: Double, to: Double)(result: String): ValidationResult = {
    if (isFloatingNumber(result).valid) {
      val value = result.toDouble
      if (value >= from && value <= to) {
        return ValidationSuccess
      }
    }

    ValidationFailure(s"Value must be between $from and $to.")
  }

  def positiveInteger(result: String): ValidationResult = {
    if (numeric(result).valid) {
      if (result.toInt > 0) {
        return ValidationSuccess
      }
    }

    ValidationFailure("Value must be a positive integer!")
  }

  def inList(options: Seq[String],
             errorMsg: String = "Value %s is not one of %s.")(result: String): ValidationResult = {
    options.find(_ == result) match {
      case Some(mtch) => ValidationSuccess
      case None => new ValidationFailure(errorMsg.format(result, options.mkString(", ")))
    }
  }

  def notInList(options: Seq[String],
                errorMsg: String = "Value %s is one of %s.")(result: String): ValidationResult = {
    options.find(_ == result) match {
      case None => ValidationSuccess
      case Some(mtch) => new ValidationFailure(errorMsg.format(result, options.mkString(", ")))
    }
  }

  def isURL()(result: String) = {
    try {
      val url = new URL(result)
      url.toURI

      ValidationSuccess
    } catch {
      case e: Exception => new ValidationFailure(s"$result, Not a valid url!")
    }
  }

  def existingFile()(result: String) = {
    val file = new File(result)
    file.exists() match {
      case true => ValidationSuccess
      case false => new ValidationFailure(s"File ${file.getAbsolutePath} does not exist!")
    }
  }

  def nonExistingFile()(result: String) = {
    val file = new File(result)
    file.exists() match {
      case false => ValidationSuccess
      case true => new ValidationFailure(s"File ${file.getAbsolutePath} already exists!")
    }
  }

  def nonExistingFile(directory: File, fileNamePattern: String = "%s")(result: String) = {
    directory.listFiles.find(_ == fileNamePattern.format(result)) match {
      case Some(file) => new ValidationFailure(s"File ${file.getAbsolutePath} already exists.")
      case None => ValidationSuccess
    }
  }

  def lengthBetween(min: Int, max: Int)(result: String) = {
    if (min >= 0 && result.length < min) {
      new ValidationFailure(s"Minimum length is $min.")
    } else if (max > 0 && result.length > max) {
      new ValidationFailure(s"Maximum length is $max.")
    } else {
      ValidationSuccess
    }
  }

  def onlyLowerCase(result: String) =
    limitedCharacterSet("abcdefghijklmnopqrstuvwxyz")(result)

  def onlyLetters(result: String) =
    limitedCharacterSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")(result)

  def onlyNumbers(result: String) =
    limitedCharacterSet("0123456789")(result)

  def limitedCharacterSet(alphabet: String)(result: String): ValidationResult = {
    for (char <- result) {
      if (!alphabet.contains(char)) {
        return new ValidationFailure(s"Please only use the following characters: $alphabet")
      }
    }

    ValidationSuccess
  }

  def or(validator1: String => ValidationResult,
         validator2: String => ValidationResult)(result: String): ValidationResult = {
    val res1 = validator1(result)
    if (!res1.valid) {
      val res2 = validator2(result)
      if (!res2.valid) {
        return new ValidationFailure(
          s"""Correct either of the following issues:
             |  ${res1.message}
             |  ${res2.message}""".stripMargin)
      }
    }

    ValidationSuccess
  }

  def listOf(validator: String => ValidationResult)(result: String): ValidationResult = {
    val elements = result.split(",").map(validator)
    if (elements.forall(_.valid)) {
      ValidationSuccess
    } else {
      new ValidationFailure("Some of the selections are invalid: " +
        elements.filter(!_.valid).map(_.message).mkString(","))
    }
  }

  def chooseNCommaSeparated(options: Seq[String], reqCount: Int)(result: String): ValidationResult = {
    require(options.size > reqCount, s"options supplied must be greater than required items to choose")

    val values = result.split(",")
    if (values.size != reqCount && reqCount > 0) {
      ValidationFailure(s"You chose ${values.size} items, but required to choose $reqCount")
    } else {
      val trimmedValues = values.map(_.trim)
      if (trimmedValues.forall(options.contains)) {
        ValidationSuccess
      } else {
        val invalidChosenOptions = trimmedValues.filterNot(options.contains)
        ValidationFailure(s"Invalid options chosen ${invalidChosenOptions.mkString(",")}")
      }
    }
  }
}
