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

import com.sumologic.shellbase.ShellStringSupport._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ShellStringSupportTest extends CommonWordSpec {
  "ShellStringSupport" should {
    "calculate the visible length of a string without escapes" in {
      val s = "a string"
      s.visibleLength should be(8)
    }

    "calculate the visible length of a string with escapes" in {
      val s = ShellColors.red("a red string")
      s.visibleLength should be(12)
    }

    "trim a string with escapes" in {
      val s = s"a ${ShellColors.red("red")} string"
      s.escapedTrim(6) should be(s"a ${ShellColors.red("red")} ")
    }
  }
}
