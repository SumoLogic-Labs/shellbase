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

import org.apache.commons.io.IOUtils

import scala.io.Source

class ScriptRenderer(file: File, args: Array[String]) {

  def getLines: Seq[String] = {
    val prefix = file.getName + ("_" * Math.max(0, 3 - file.getName.length))
    val output = File.createTempFile(prefix, "script")
    output.deleteOnExit()
    val reader = new FileReader(file)
    val writer = new FileWriter(output)
    try {
      VelocityRenderer.render(argsToMap, reader, writer)
    } finally {
      IOUtils.closeQuietly(reader)
      IOUtils.closeQuietly(writer)
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
