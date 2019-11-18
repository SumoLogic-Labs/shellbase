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
package com.sumologic.shellbase.commands

import com.sumologic.shellbase.CommonWordSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SleepCommandTest extends CommonWordSpec {

  "SleepCommand" should {
    "let you sleep a bit" in {
      new SleepCommand().executeLine(List("30")) should be(true)
    }

    "inform you about missing arguments for sleep" in {
      new SleepCommand().executeLine(List.empty) should be(false)
    }

    "work in verbose mode" in {
      new SleepCommand().executeLine(List("-v", "30")) should be(true)
    }
  }

}
