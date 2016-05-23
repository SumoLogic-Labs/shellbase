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

import java.io.File

import com.sumologic.shellbase.timeutil.TimedBlock
import com.sumologic.shellbase.{ScriptRenderer, ShellCommand}
import jline.console.completer.{ArgumentCompleter, Completer, NullCompleter, StringsCompleter}
import org.apache.commons.cli.{CommandLine, Options}

import scala.collection.JavaConversions._

class RunScriptCommand(scriptDir: File, scriptExtension: String, runCommand: String => Boolean,
                       parseLine: String => List[String]) extends ShellCommand("run_script",
  "Run the script from the specified file.", List("script")) {

  override def maxNumberOfArguments = -1

  def execute(cmdLine: CommandLine): Boolean = {

    val continue = cmdLine.hasOption("continue")

    var scriptFile: File = null
    val args: Array[String] = cmdLine.getArgs

    if (args.length < 1) {
      printf("Please specify a script to run!")
      return false
    }

    val scriptFileName = args(0)

    for (pattern <- List("scripts/%s.dsh", "scripts/%s", "%s")) {
      val tmp = new File(pattern.format(scriptFileName))
      if (tmp.exists) {
        scriptFile = tmp
      }
    }

    if (scriptFile == null) {
      print(s"Could not find the script $scriptFileName! Please make sure the script file exists locally.")
      return false
    }

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
  }

  override def argCompleter: Completer = {
    if (scriptDir.exists) {
      var scriptNames = scriptDir.listFiles.filter(_.isFile).map(_.getName)
      if (scriptExtension != null) {
        scriptNames = scriptNames.filter(_.endsWith(scriptExtension))
      }
      new ArgumentCompleter(List(new StringsCompleter(scriptNames: _*), new NullCompleter))
    } else {
      new NullCompleter
    }
  }

  override def addOptions(opts: Options) = {
    opts.addOption("c", "continue", false, "Continue even if there was a failure in execution.")
  }
}
