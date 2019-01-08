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

import java.util
import java.util.concurrent.Semaphore

import com.sumologic.shellbase.notifications.{InMemoryShellNotificationManager, ShellNotification, ShellNotificationManager}
import jline.console.completer.CandidateListCompletionHandler
import org.apache.commons.cli.CommandLine
import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner
import sun.misc.Signal
import org.mockito.Mockito._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ShellBaseTest extends CommonWordSpec with Eventually {

  "ShellBase.main" should {
    "call init method and bubble exceptions" in {
      class VerySpecificException extends Exception
      intercept[VerySpecificException] {
        new ShellBase("test") {
          override def init(cmdLine: CommandLine): Boolean = {
            throw new VerySpecificException
          }
        }.main(Array.empty)
      }
    }

    "exit after executing all the command line arguments" in {
      // given
      var exitCode: Int = -1
      class ShellOneCanExit extends ShellBase("test") {
        override def commands: Seq[ShellCommand] = Seq(new DummyCommand("callme"))

        override def exitShell(exitValue: Int): Unit = {
          exitCode = exitValue
        }
      }
      val sut = spy(new ShellOneCanExit)

      // when
      sut.main(Seq("callme").toArray)

      // then
      exitCode should be(0)
    }

    "exit with non-zero exit code after executing a command line argument command fails" in {
      // given
      var exitCode: Int = -1
      class ShellOneCanExit extends ShellBase("test") {
        override def commands: Seq[ShellCommand] = Seq(new DummyFailingCommand("callme"))

        override def exitShell(exitValue: Int): Unit = {
          exitCode = exitValue
        }
      }
      val sut = spy(new ShellOneCanExit)

      // when
      sut.main(Seq("callme").toArray)

      // then
      exitCode should be(1)
    }
  }

  "ShellBase.validateCommands" should {
    "throw an exception" when {
      "two commands have identical names" in {
        the[DuplicateCommandException] thrownBy {
          runValidation(List(new DummyCommand("doop"), new DummyCommand("doop")))
        }
      }

      "two commands have identical aliases" in {
        the[DuplicateCommandException] thrownBy {
          runValidation(List(new DummyCommand("one", List("doop")), new DummyCommand("two", List("doop"))))
        }
      }

      "the name of one command matches the alias of another" in {
        the[DuplicateCommandException] thrownBy {
          runValidation(List(new DummyCommand("one", List("doop")), new DummyCommand("doop")))
        }
      }

      "a command duplicates the name of one of the built ins" in {
        the[DuplicateCommandException] thrownBy {
          runValidation(List(new DummyCommand("clear")))
        }
      }

      "a command's alias duplicates the name of one of the built ins" in {
        the[DuplicateCommandException] thrownBy {
          runValidation(List(new DummyCommand("yoyo", List("clear"))))
        }
      }

      "a nested ShellCommandSet has duplicate commands" in {
        the[DuplicateCommandException] thrownBy {
          val commandSet = new ShellCommandSet("yoyo", "") {
            commands += new DummyCommand("one", List("foo-bar"))
            commands += new DummyCommand("foo_bar")
          }
          runValidation(List(commandSet))
        }
      }

      "two commands have identical sanitized names" in {
        the[DuplicateCommandException] thrownBy {
          runValidation(List(new DummyCommand("foo-bar"), new DummyCommand("foo_bar")))
        }
      }

      "two commands have identical sanitized aliases" in {
        the[DuplicateCommandException] thrownBy {
          runValidation(List(new DummyCommand("one", List("foo-bar")), new DummyCommand("two", List("foo_bar"))))
        }
      }

      "the sanitized name of one command matches the alias of another" in {
        the[DuplicateCommandException] thrownBy {
          runValidation(List(new DummyCommand("one", List("foobar")), new DummyCommand("foo-bar")))
        }
      }
    }
  }

  "ShellCommandAlias" should {
    "allow another command to be executed" in {
      val original = new DummyCommand("original")
      val alias = new ShellCommandAlias(original, "alias1", List("alias2"))

      val sut = setUpShellBase(List(original, alias))

      def thisShouldRunIt(line: String) {
        original.executed = false
        sut.runCommand(line)
        original.executed should be(true)
        original.executed = false
      }

      thisShouldRunIt("original")
      thisShouldRunIt("alias1")
      thisShouldRunIt("alias2")
    }

    "allow another command with different separator to be executed" in {
      val original = new DummyCommand("original")
      val alias = new ShellCommandAlias(original, "dummy-alias", List("another_alias"))

      val sut = setUpShellBase(List(original, alias))

      def thisShouldRunIt(line: String) {
        original.executed = false
        sut.runCommand(line)
        original.executed should be(true)
        original.executed = false
      }

      thisShouldRunIt("original")
      thisShouldRunIt("dummy_alias")
      thisShouldRunIt("another-alias")
    }

    "allow another command with reduced separator to be executed" in {
      val original = new DummyCommand("original")
      val alias = new ShellCommandAlias(original, "dummy-alias", List("another_alias"))

      val sut = setUpShellBase(List(original, alias))

      def thisShouldRunIt(line: String) {
        original.executed = false
        sut.runCommand(line)
        original.executed should be(true)
        original.executed = false
      }

      thisShouldRunIt("original")
      thisShouldRunIt("dummyalias")
      thisShouldRunIt("anotheralias")
    }
  }

  "ShellBase.runCommand" should {
    "support && within single quotes" in {
      val commandList = List(new DummyCommand("a"), new DummyCommand("c"))
      val sut = setUpShellBase(commandList)
      sut.runCommand("a && echo 'begin && end' && c")
      assert(commandList(0).executed === true)
      assert(commandList(1).executed === true)
    }

    "support && within double quotes" in {
      val commandList = List(new DummyCommand("a"), new DummyCommand("c"))
      val sut = setUpShellBase(commandList)
      sut.runCommand("a && echo \"begin && end\" && c")
      assert(commandList(0).executed === true)
      assert(commandList(1).executed === true)

    }

    "run all commands separated by &&" in {
      val commandList = List(new DummyCommand("a"), new DummyCommand("b"), new DummyCommand("c"))
      val sut = setUpShellBase(commandList)
      sut.runCommand("a && b && c")
      commandList.foreach { command =>
        withClue(s"at ${command.name} ") {
          assert(command.executed === true)
        }
      }
    }

    "run notifications" in {
      import scala.collection.mutable
      val notifications = new mutable.MutableList[(String,String)]
      val notification = new ShellNotification {
        override def name: String = "test"

        override def notify(title: String, message: String): Unit = notifications += ((title, message))
      }
      val shell = setUpShellBase(
        List(),
        new InMemoryShellNotificationManager("tsh", Seq(notification), true)
      )
      shell.runCommand("echo \"hello world\"")
      assert(notifications.toList == List(("tsh", "Command finished successfully: 'echo hello world'")))
    }

    "not run notifications for the 'notifications' command set" in {
      import scala.collection.mutable
      val notifications = new mutable.MutableList[(String, String)]
      val notification = new ShellNotification {
        override def name: String = "test"

        override def notify(title: String, message: String): Unit = notifications += ((title, message))
      }
      val shell = setUpShellBase(
        List(),
        new InMemoryShellNotificationManager("tsh", Seq(notification), true)
      )
      shell.runCommand("notifications disable test")
      shell.runCommand("notifications enable test")
      assert(notifications.isEmpty)
    }

    "run all commands separated by && until a failing command" in {
      val commandList = List(new DummyCommand("a"), new DummyFailingCommand("x"), new DummyCommand("c"))
      val sut = setUpShellBase(commandList)
      sut.runCommand("a && x && c")
      assert(commandList(0).executed === true)
      assert(commandList(1).executed === true)
      assert(commandList(2).executed === false)
    }

    "run fine even when verbose mode is enabled" in {
      val commandList = List(new DummyCommand("a"), new DummyFailingCommand("x"), new DummyCommand("c"))
      val sut = setUpShellBase(commandList)
      sut.runCommand("a && x && c")

      _verboseMode = true

      assert(commandList(0).executed === true)
      assert(commandList(1).executed === true)
      assert(commandList(2).executed === false)
    }
  }

  "ShellBase thread-handling" should {
    "interrupt a long running command when Ctrl-C is pressed" in {
      // given
      val cmd = new LongRunningCommand("sleepysleep", durationInMillis = 100000L)
      val sut = setUpShellBase(List(cmd))
      val sutThread = new Thread {
          override def run(): Unit = {
            sut.runKillableCommand("sleepysleep")
          }
      }
      sutThread.start()

      // when
      cmd.started.acquire()
      Thread.sleep(500L) // that's longer than the 200ms initial quick-command period defined in runKillableCommand()
      Signal.raise(new Signal("INT"))

      // test
      eventually { sutThread.isAlive should be (false) }
      sut.interruptKeyMonitor.isMonitoring should be (false)
      cmd.completedSuccessfully should be (false)
    }
  }

  "ShellBase auto-completion" should {

    def complete(input: String, cursor: Int): (Int, List[String]) = {
      val shell = new AutoCompleteTestShell
      shell.initializeCommands()
      val completer = shell.rootSet.argCompleter
      val candidatesPlaceholder = new util.LinkedList[CharSequence]()
      val startPos = completer.complete(input, cursor, candidatesPlaceholder)
      val candidates = candidatesPlaceholder.asScala

      // adapted/copied logic from CandidateListCompletionHandler for full completion
      if (candidatesPlaceholder.size == 1 && cursor == input.length && !candidates.head.toString.endsWith(" ")) {
        startPos -> List(candidates.head.toString + " ")
      } else {
        startPos -> candidates.map(_.toString).toList
      }
    }

    class AutoCompleteTestShell extends ShellBase("AutoCompleteTestShell") {

      class ApplesCommand extends DummyCommand("apples", List("app"))

      class PersimmonsCommandSet extends ShellCommandSet("persimmons", "", List("pom"))

      class BananasCommandSet extends ShellCommandSet("bananas", "", List("ban")) {
        commands += new ShellCommandSet("yellow", "") {
          commands += new DummyCommand("banners", List("ban"))
          commands += new DummyCommand("boxes", List())
          commands += new DummyCommand("eat_all", List())
        }
      }

      override def commands: Seq[ShellCommand] = List[ShellCommand](
        new ApplesCommand(),
        new BananasCommandSet(),
        new PersimmonsCommandSet()
      )
    }

    "return zero candidates when there no valid matches" in {
      complete("pomegranates", 0)._2 should be('empty)
      complete("pomegranates", 3)._2 should be('empty)
      complete("pomegranates", 13)._2 should be('empty)
      complete("ban NoMatch", 10)._2 should be('empty)
      complete("ban yellow NoMatch", 17)._2 should be('empty)
      complete("ban yell boxes", 15)._2 should be('empty)
    }

    "suggest completions for an unfinished token" in {
      complete("ba", 2) should equal(0 -> List("ban", "bananas"))
      complete("ban", 2) should equal(0 -> List("ban", "bananas"))
      complete("ban", 3) should equal(0 -> List("ban", "bananas"))
      complete("ban yellow b", 12) should equal(11 -> List("ban", "banners", "boxes"))
    }

    "append a space when the choice is unambiguous" in {
      complete("ban yell", 8) should equal(4 -> List("yellow "))
      complete("ban yellow", 10) should equal(4 -> List("yellow "))
      complete("ban yellow ea", 13) should equal(11 -> List("eat-all "))
      complete("pom", 3) should equal(0 -> List("pom "))
    }

    "not append a space when the choice is unambiguous, but the cursor wasn't at the end" in {
      complete("persimmons", 3) should equal(0 -> List("persimmons"))
    }

    "suggest candidate next tokens after the current one" in {
      complete("ban ", 4) should equal(4 -> List("?", "help", "yellow"))
      complete("ban yellow ", 11) should equal(11 -> List("?", "help", "ban", "banners", "boxes", "eat-all"))
    }

    "complete properly when there are more characters after the one being completed" in {
      complete("ban yell boxes", 8) should equal(4 -> List("yellow"))
      complete("ban yellow b -skip none --permanent", 12) should equal(11 -> List("ban", "banners", "boxes"))
      complete("ban yellow bags", 13)._2 should be('empty)
    }

    "account for extra whitespaces" in {
      complete("   app", 6) should equal(3 -> List("app", "apples"))
      complete("   bananas", 10) should equal(3 -> List("bananas "))
      complete("   bananas ", 11) should equal(11 -> List("?", "help", "yellow"))
      complete("   bananas  ", 12) should equal(12 -> List("?", "help", "yellow"))
      complete("   ban    yellow  banners", 25) should equal(18 -> List("banners "))
    }
  }

  private var _verboseMode = false
  def setUpShellBase(commandList: Seq[ShellCommand]): ShellBase = {
    _verboseMode = false

    val res = new ShellBase("test") {
      /**
        * Return the list of commands.
        */
      override def commands = commandList

      override def verboseMode = _verboseMode
    }
    res.initializeCommands()
    res
  }

  def setUpShellBase(commandList: Seq[ShellCommand], shellNotificationManager: ShellNotificationManager): ShellBase = {
    _verboseMode = false

    val res = new ShellBase("test") {
      override def commands: Seq[ShellCommand] = commandList

      override def verboseMode: Boolean = _verboseMode

      override lazy val notificationManager: ShellNotificationManager = shellNotificationManager
    }
    res.initializeCommands()
    res
  }

  def runValidation(commandList: Seq[ShellCommand]) {
    setUpShellBase(commandList).validateCommands()
  }
}


class DummyCommand(name: String, aliases: List[String] = List()) extends ShellCommand(name, "dummy", aliases) {

  var executed = false

  def execute(cmdLine: CommandLine) = {
    executed = true
    true
  }
}

class DummyFailingCommand(name: String, aliases: List[String] = List()) extends DummyCommand(name, aliases) {
  override def execute(cmdLine: CommandLine) = {
    super.execute(cmdLine)
    false
  }
}

class LongRunningCommand(name: String, durationInMillis: Long, aliases: List[String] = List())
  extends ShellCommand(name, "dummy", aliases) {

  var completedSuccessfully = false
  val started = new Semaphore(1)
  started.acquire()

  def execute(cmdLine: CommandLine) = {
    started.release()
    Thread.sleep(durationInMillis)
    completedSuccessfully = true
    true
  }
}
