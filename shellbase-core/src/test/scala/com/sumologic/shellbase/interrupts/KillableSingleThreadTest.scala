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
package com.sumologic.shellbase.interrupts

import com.sumologic.shellbase.CommonWordSpec
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class KillableSingleThreadTest extends CommonWordSpec {
  "KillableSingleThread" should {
    "provide the result of a thread" in {
      val sut = new KillableSingleThread(
        "hello"
      )

      sut.start()

      Await.result(sut.future, Duration.apply(10, "seconds")) should be("hello")
    }

    "let you kill a stuck thread" in {
      val sut = new KillableSingleThread(
        while (true) {
          Thread.sleep(500)
        }
      )

      sut.start()

      sut.kill(Duration.apply(500, "ms"))
    }
  }

}
