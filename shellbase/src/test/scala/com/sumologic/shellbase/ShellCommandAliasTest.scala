package com.sumologic.shellbase

import com.sumologic.shellbase.cmdline.RichCommandLine._
import com.sumologic.shellbase.cmdline.{CommandLineOption, RichCommandLine}
import org.apache.commons.cli.{CommandLine, Options}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ShellCommandAliasTest extends CommonWordSpec {
  "ShellCommandAlias" should {
    "execute original command" in {
      val cmd = new DummyCommand("almond")
      val subtree = new ShellCommandAlias(cmd, "better_almond", List())
      subtree.execute(null)

      cmd.executed should be(true)
    }

    "have same options" in {
      val option = new CommandLineOption("e", "example", true, "An example in test")
      val cmd = new ShellCommand("almond", "Just for testing") {

        def execute(cmdLine: CommandLine) = {
          cmdLine.get(option).isDefined
        }

        override def addOptions(opts: Options) {
          opts.addOption(option)
        }
      }
      val subtree = new ShellCommandAlias(cmd, "alias", List())

      subtree.parseOptions(List("--example", "a")).hasOption("example") should be(true)
      cmd.parseOptions(List("--example", "a")).hasOption("example") should be(true)
      val cmdLine = mock(classOf[CommandLine])
      when(cmdLine.hasOption("example")).thenReturn(true)
      when(cmdLine.getOptionValue("example")).thenReturn("return")
      subtree.execute(cmdLine)
    }
  }
}
