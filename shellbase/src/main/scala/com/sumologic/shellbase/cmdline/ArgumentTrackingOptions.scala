package com.sumologic.shellbase.cmdline

import java.util.concurrent.ConcurrentLinkedQueue

import org.apache.commons.cli.Options

import scala.collection.JavaConversions._

class ArgumentTrackingOptions extends Options {
  private[this] val arguments = new ConcurrentLinkedQueue[CommandLineArgument]

  def addArgument(arg: CommandLineArgument) {
    arguments.add(arg)
  }

  def getArguments: Array[CommandLineArgument] = arguments.toList.toArray
}
