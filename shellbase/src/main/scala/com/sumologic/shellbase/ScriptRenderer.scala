package com.sumologic.shellbase

import java.io._

import resource._

import scala.io.Source

class ScriptRenderer(file: File, args: Array[String]) {

  def getLines: Seq[String] = {
    val output = File.createTempFile(file.getName, "script")
    output.deleteOnExit()
    managed(new FileReader(file)).and(managed(new FileWriter(output))).acquireAndGet { case (reader, writer) =>
      VelocityRenderer.render(argsToMap, reader, writer)
    }
    Source.fromFile(output).getLines().toSeq
  }

  private[shellbase] def argsToMap: Map[String, String] = {
    args.map(s => {
      require(s.contains("="), s"Argument $s does not contain = sign!")
      val splitted = s.split("=")
      require(splitted.size == 2, s"Argument $s contains more than on =!")
      splitted(0) -> splitted(1)
    }).toMap
  }

}
