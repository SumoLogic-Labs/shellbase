package com.sumologic.shellbase

import java.util

import org.apache.commons.cli.CommandLine
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ShellBaseTest extends CommonWordSpec {

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
            commands += new DummyCommand("one", List("doop"))
            commands += new DummyCommand("doop")
          }
          runValidation(List(commandSet))
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

    "run all commands separated by && until a failing command" in {
      val commandList = List(new DummyCommand("a"), new DummyFailingCommand("x"), new DummyCommand("c"))
      val sut = setUpShellBase(commandList)
      sut.runCommand("a && x && c")
      assert(commandList(0).executed === true)
      assert(commandList(1).executed === true)
      assert(commandList(2).executed === false)
    }
  }

  "ShellBase auto-completion" should {

    def complete(input: String, cursor: Int): (Int, List[String]) = {
      val shell = new AutoCompleteTestShell
      shell.initializeCommands()
      val completer = shell.rootSet.argCompleter
      val candidates = new util.LinkedList[CharSequence]()
      val startPos = completer.complete(input, cursor, candidates)
      startPos -> candidates.asScala.map(_.toString).toList
    }

    class AutoCompleteTestShell extends ShellBase("AutoCompleteTestShell") {

      class ApplesCommand extends DummyCommand("apples", List("app"))

      class PersimmonsCommandSet extends ShellCommandSet("persimmons", "", List("pom"))

      class BananasCommandSet extends ShellCommandSet("bananas", "", List("ban")) {
        commands += new ShellCommandSet("yellow", "") {
          commands += new DummyCommand("banners", List("ban"))
          commands += new DummyCommand("boxes", List())
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
      complete("ban yellow b", 12) should equal(11 -> List("ban", "banners", "boxes "))
    }

    "append a space when the choice is unambiguous" in {
      complete("ban yell", 8) should equal(4 -> List("yellow "))
      complete("ban yellow", 10) should equal(4 -> List("yellow "))
      complete("pom", 3) should equal(0 -> List("pom "))
      complete("persimmons", 3) should equal(0 -> List("persimmons "))
    }

    "suggest candidate next tokens after the current one" in {
      complete("ban ", 4) should equal(4 -> List("?", "help", "yellow "))
      complete("ban yellow ", 11) should equal(11 -> List("?", "help", "ban", "banners", "boxes "))
    }

    "complete properly when there are more characters after the one being completed" in {
      complete("ban yell boxes", 8) should equal(4 -> List("yellow "))
      complete("ban yellow b -skip none --permanent", 12) should equal(11 -> List("ban", "banners", "boxes "))
      complete("ban yellow bags", 13)._2 should be('empty)
    }

    "account for extra whitespaces" in {
      complete("   app", 6) should equal(3 -> List("app", "apples"))
      complete("   bananas", 10) should equal(3 -> List("bananas "))
      complete("   bananas ", 11) should equal(11 -> List("?", "help", "yellow "))
      complete("   bananas  ", 12) should equal(12 -> List("?", "help", "yellow "))
      complete("   ban    yellow  banners", 25) should equal(18 -> List("banners "))
    }
  }

  def setUpShellBase(commandList: Seq[ShellCommand]): ShellBase = {
    val res = new ShellBase("test") {
      /**
        * Return the list of commands.
        */
      override def commands = commandList
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
