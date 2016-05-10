package com.sumologic.shellbase

import java.io._
import java.util.Properties

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.{Velocity, VelocityEngine}
import resource._

import scala.collection.JavaConversions._

object VelocityRenderer {

  val props = new Properties()
  props.setProperty("runtime.references.strict", "true")
  props.setProperty("velocimacro.arguments.strict", "true")
  props.setProperty("velocimacro.permissions.allow.inline.local.scope", "true")
  props.setProperty("directive.foreach.skip.invalid", "false")
  props.setProperty("runtime.log.logsystem.log4j.logger", "org.apache.velocity")
  props.setProperty("resource.loader", "class,file")
  props.setProperty("class.resource.loader.description", "Velocity Classpath Resource Loader")
  props.setProperty("class.resource.loader.class",
    "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
  Velocity.init(props)

  def render(map: Iterable[(String, AnyRef)], reader: Reader, writer: Writer): Unit = {
    val context = new VelocityContext()
    map.foreach { pair =>
      context.put(pair._1, pair._2)
    }
    Velocity.evaluate(context, writer, "ops util velocity renderer", reader)
  }

  def render(templateVars: Iterable[(String, AnyRef)], templatePath: String, outputPath: String): Unit = {
    val templateReader = new InputStreamReader(getClass.getClassLoader.getResourceAsStream(templatePath))
    val outputWriter = new OutputStreamWriter(new FileOutputStream(outputPath))
    render(templateVars, templateReader, outputWriter)
    templateReader.close()
    outputWriter.close()
  }

  def createScriptFromTemplate(scriptResource: String,
                               variables: Map[AnyRef, AnyRef] = Map[AnyRef, AnyRef]()): File = {
    val engine = new VelocityEngine()
    engine.setProperty("resource.loader", "class")
    engine.setProperty("class.resource.loader.description", "Velocity Classpath Resource Loader")
    engine.setProperty("class.resource.loader.class",
      "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
    engine.init()

    val template = engine.getTemplate(scriptResource)

    val tempFile = File.createTempFile(".tmp", ".sh")

    val modifiableVariables = new java.util.HashMap[AnyRef, AnyRef]()
    modifiableVariables ++= variables

    for (writer <- managed(new FileWriter(tempFile))) {
      template.merge(new VelocityContext(modifiableVariables), writer)
    }

    tempFile
  }

}
