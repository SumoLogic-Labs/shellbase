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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ShellColorsTest extends CommonWordSpec {
  "ShellColors" should {
    "not error for each color and contain original text" in {
      ShellColors.black("test") should include("test")
      ShellColors.blue("test") should include("test")
      ShellColors.cyan("test") should include("test")
      ShellColors.green("test") should include("test")
      ShellColors.magenta("test") should include("test")
      ShellColors.red("test") should include("test")
      ShellColors.white("test") should include("test")
      ShellColors.yellow("test") should include("test")
    }
  }
}
