package com.sumologic.shellbase

import org.apache.commons.cli.{CommandLine, Options}

/**
  * Allows hooking a pre-existing shell command into another place in the command
  * hierarchy.
  */
class ShellCommandAlias(original: ShellCommand,
                        name: String,
                        aliases: List[String],
                        deprecated: Boolean = false)
  extends ShellCommand(name, original.helpText, aliases, deprecated) {

  override def maxNumberOfArguments = original.maxNumberOfArguments

  def execute(cmdLine: CommandLine) = original.execute(cmdLine)

  override def addOptions(opts: Options) {
    original.addOptions(opts)
  }
}
