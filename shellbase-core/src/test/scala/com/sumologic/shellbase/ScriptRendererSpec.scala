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

import java.io.File

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ScriptRendererSpec extends CommonWordSpec {
  "ScriptRenderer" should {
    "convert the arguments to a map" in {
      val command = new ScriptRenderer(null, Array("key=value", "key1=value1"))
      val props = command.argsToMap
      props("key") should be("value")
      props("key1") should be("value1")
    }
    "get the lines of non velocity script" in {
      val parser = new ScriptRenderer(new File("src/test/resources/scripts/novelocity"), Array[String]())
      val lines: Seq[String] = parser.getLines
      lines should contain("do something")
      lines should contain("exit")
    }
    "get the lines of velocity script with keys replaced with values" in {
      val parser = new ScriptRenderer(new File("src/test/resources/scripts/velocity"), Array("key1=value1", "key2=value2"))
      val lines: Seq[String] = parser.getLines
      lines should contain("do something value1")
      lines should contain("do something value2")
      lines should contain("exit")
    }
  }
}
