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
class TimeFormatsTest extends CommonWordSpec {
  import TimeFormats._

  "TimeFormats.parseTersePeriod" should {
    "return a time" when {
      "a time in millis is passed" in {
        parseTersePeriod("1234") should be (Some(1234))
      }

      "a terse period is passed" in {
        val seconds30 = 30 * 1000
        val minutes2 = seconds30 * 4
        parseTersePeriod("2m30s") should be (Some(minutes2 + seconds30))
      }
    }

    "return None" when {
      "null is passed" in {
        parseTersePeriod(null) should be (None)
      }

      "empty string is passed" in {
        parseTersePeriod("") should be (None)
      }

      "unparsable is passed" in {
        parseTersePeriod("humbug") should be (None)
      }
    }
  }

  "TimeFormats.formatAsTersePeriod" should {
    "return 0 when span is 0" in {
      formatAsTersePeriod(0) should be ("0")
    }

    "format as milliseconds when span is less than 1000" in {
      formatAsTersePeriod(100) should be ("100ms")
      formatAsTersePeriod(-100) should be ("-100ms")
      formatAsTersePeriod(1) should be ("1ms")
    }

    "format as seconds when span is 1000" in {
      formatAsTersePeriod(1000) should be ("1s")
    }

    "format properly for different values" in {
      formatAsTersePeriod(60*1000) should be ("1m")
      formatAsTersePeriod(60*60*1000) should be ("1h")
      formatAsTersePeriod(24*60*60*1000) should be ("1d")
      formatAsTersePeriod(7*24*60*60*1000) should be ("7d")

      val millis_3d6h5m4s10ms = 3*24*60*60*1000 + 6*60*60*1000 + 5*60*1000 + 4*1000 + 10
      formatAsTersePeriod(millis_3d6h5m4s10ms) should be ("3d6h5m4s")
    }

    "not fail when time span is large" in {
      formatAsTersePeriod(Long.MaxValue - 1) should be (errorString)
    }
  }
}
