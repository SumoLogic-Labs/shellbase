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

import java.io.{File, FilenameFilter}

import com.sumologic.shellbase.timeutil.TimedBlock
import com.sumologic.shellbase.{ScriptRenderer, ShellBase, ShellCommand}
import jline.console.completer.{ArgumentCompleter, Completer, NullCompleter, StringsCompleter}
import org.apache.commons.cli.{CommandLine, Options}

import scala.collection.JavaConverters._

class RunScriptCommand(scriptDirs: List[File], scriptExtension: String, runCommand: String => Boolean,
                       parseLine: String => List[String] = ShellBase.parseLine) extends ShellCommand("run-script",
  "Run the script from the specified file.", List("script")) {

  override def maxNumberOfArguments: Int = -1

  def execute(cmdLine: CommandLine): Boolean = {

    val continue = cmdLine.hasOption("continue")

    val args: Array[String] = cmdLine.getArgs

    if (args.length < 1) {
      println("Please specify a script to run!")
      return false
    }

    val scriptFileName = args(0)

    val scriptFiles: List[File] = findScripts {
      List(
        s"$scriptFileName.$scriptExtension",
        s"$scriptFileName.dsh", // NOTE(konstantin, 2017-04-02): "dsh" kept for compatibility reasons
        s"$scriptFileName"
      ).contains
    } ++ List(new File(scriptFileName)).filter(f => f.exists && f.isFile && f.canRead) // respect absolute paths too

    scriptFiles match {
      case scriptFile :: _ =>
        // Execute the script, line by line.
        TimedBlock(s"Executing script $scriptFileName", println(_)) {
          val scriptLines = new ScriptRenderer(scriptFile, args.tail).
            getLines.filterNot(parseLine(_).isEmpty)
          require(scriptLines.nonEmpty, s"No non-comment lines found in $scriptFileName")
          for (line <- scriptLines) {
            val success = runCommand(line)
            if (!continue && !success) {
              return false
            }
          }
        }

        true

      case _ =>
        println(s"Could not find the script $scriptFileName! Please make sure the script file exists locally.")
        false
    }
  }

  override def argCompleter: Completer = {
    val suffix = s".$scriptExtension"
    val scriptNames = findScripts(name => scriptExtension == null || name.endsWith(suffix)).map(_.getName)
    new ArgumentCompleter(List(new StringsCompleter(scriptNames: _*), new NullCompleter): _*)
  }

  private def findScripts(fileNameFilter: String => Boolean): List[File] = for (
      scriptDir <- scriptDirs
      if scriptDir.exists();
      file <- scriptDir.listFiles(new FilenameFilter {
        override def accept(dir: File, name: String): Boolean = fileNameFilter.apply(name)
      })
      if file.isFile && file.canRead
    ) yield file

  override def addOptions(opts: Options): Unit = {
    opts.addOption("c", "continue", false, "Continue even if there was a failure in execution.")
  }
}
