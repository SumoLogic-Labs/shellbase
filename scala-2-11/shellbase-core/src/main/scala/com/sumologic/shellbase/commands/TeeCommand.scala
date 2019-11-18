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

import java.io.{FileOutputStream, PrintStream}

import com.sumologic.shellbase.ShellCommand
import com.sumologic.shellbase.cmdline.RichCommandLine._
import com.sumologic.shellbase.cmdline.{CommandLineArgument, CommandLineFlag, CommandLineOption}
import org.apache.commons.cli.{CommandLine, Options}
import org.apache.commons.io.output.TeeOutputStream

import scala.util.Try

class TeeCommand(runCommand: String => Boolean) extends ShellCommand("tee", "Forks the stdout of a command so it also prints to a file") {

  private val CommandArgument = new CommandLineArgument("command", 0, true)
  private val OutputFileOption = new CommandLineOption("o", "outputFile", false, "Filename of output file (defaults to ~/tee.out)")
  private val AppendFileFlag = new CommandLineFlag("a", "append", "Append the output to the file rather than overwriting it")

  override def maxNumberOfArguments = 1

  override def addOptions(opts: Options) {
    opts += CommandArgument
    opts += OutputFileOption
    opts += AppendFileFlag
  }

  import com.sumologic.shellbase.ShellBase.SubCommandExtractor

  def execute(cmdLine: CommandLine) = {
    val outputFile = cmdLine.get(OutputFileOption).getOrElse(System.getProperty("user.home") + s"/tee.out")
    val appendFile = cmdLine.checkFlag(AppendFileFlag)

    cmdLine.get(CommandArgument) match {
      case Some(SubCommandExtractor(cmd)) =>
        val fileOut = new FileOutputStream(outputFile, appendFile)
        val newOut = new PrintStream(new TeeOutputStream(Console.out, fileOut))
        val status = Console.withOut(newOut) {
          println(s"Running `$cmd` and outputting to '$outputFile' [append=$appendFile].")
          runCommand(cmd)
        }
        Try(fileOut.close())
        status
      case badCmd =>
        println(s"Usage: tee `<command>`, but found $badCmd.")
        false
    }
  }
}
