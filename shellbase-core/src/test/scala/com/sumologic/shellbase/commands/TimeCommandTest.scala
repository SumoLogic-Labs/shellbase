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
class TimeCommandTest extends CommonWordSpec {
  "TimeCommand" should {
    "execute a subcommand and propagate exit code" in {
      var calls = 0
      def callCheck(ret: Boolean)(input: String): Boolean = {
        input should be("hi")
        calls += 1
        ret
      }

      new TimeCommand(callCheck(true)).executeLine(List("`hi`")) should be(true)
      calls should be(1)

      new TimeCommand(callCheck(false)).executeLine(List("`hi`")) should be(false)
      calls should be(2)
    }

    "degrade nicely with malformatted input" in {
      new TimeCommand(_ => true).executeLine(List.empty) should be(false)
      new TimeCommand(_ => true).executeLine(List("test")) should be(false)
    }
  }

}
