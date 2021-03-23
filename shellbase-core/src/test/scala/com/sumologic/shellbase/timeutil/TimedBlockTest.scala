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
package com.sumologic.shellbase.timeutil

import com.sumologic.shellbase.CommonWordSpec
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TimedBlockTest extends CommonWordSpec {

  "TimedBlock" should {
    "return the function result" in {
      TimedBlock("test") {
        "hello"
      } should be ("hello")
    }

    "bubble the exception" in {
      class VerySpecificException extends Exception
      intercept[VerySpecificException] {
        TimedBlock("test") {
          throw new VerySpecificException
        }
      }
    }

    "support other writers" in {
      var storageString = ""
      def recordString(str: String) = storageString += str
      TimedBlock("test", recordString) {
        "test"
      }

      storageString should not be ('empty)
    }
  }

}
