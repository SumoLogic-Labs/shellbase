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

import java.io._
import java.util.Properties
import org.apache.commons.io.IOUtils
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.{Velocity, VelocityEngine}

import java.util
import scala.collection.JavaConverters._

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

    try {
      render(templateVars, templateReader, outputWriter)
    } finally {
      IOUtils.closeQuietly(templateReader)
      IOUtils.closeQuietly(outputWriter)
    }
  }

  def createScriptFromTemplate(scriptResource: String,
                               variables: Map[String, AnyRef] = Map[String, AnyRef]()): File = {
    val engine = new VelocityEngine()
    engine.setProperty("resource.loader", "class")
    engine.setProperty("class.resource.loader.description", "Velocity Classpath Resource Loader")
    engine.setProperty("class.resource.loader.class",
      "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
    engine.init()

    val template = engine.getTemplate(scriptResource)

    val tempFile = File.createTempFile(".tmp", ".sh")

    val modifiableVariables: util.HashMap[String, AnyRef] = new java.util.HashMap[String, AnyRef]()
    modifiableVariables.asScala ++= variables

    val writer = new FileWriter(tempFile)
    try {
      template.merge(new VelocityContext(modifiableVariables), writer)
    } finally {
      IOUtils.closeQuietly(writer)
    }

    tempFile
  }

}
