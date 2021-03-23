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
package com.sumologic.shellbase.cmdline

import com.sumologic.shellbase.CommonWordSpec
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CommandLineValidatorsTest extends CommonWordSpec {
  "CommandLineValidators" should {
    "validate integers" in {
      CommandLineValidators.validInteger("1") should be(None)
      CommandLineValidators.validInteger("-1") should be(None)
      CommandLineValidators.validInteger("0") should be(None)
      CommandLineValidators.validInteger("asdf") should be('defined)
    }

    "validate positive integers" in {
      CommandLineValidators.validPositiveInteger("1") should be(None)
      CommandLineValidators.validPositiveInteger("-1") should be('defined)
      CommandLineValidators.validPositiveInteger("0") should be('defined)
      CommandLineValidators.validPositiveInteger("asdf") should be('defined)
    }

    "validate non-negative integers" in {
      CommandLineValidators.validNonNegativeInteger("1") should be(None)
      CommandLineValidators.validNonNegativeInteger("-1") should be('defined)
      CommandLineValidators.validNonNegativeInteger("0") should be(None)
      CommandLineValidators.validNonNegativeInteger("asdf") should be('defined)
    }
  }

}
