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
package com.sumologic.shellbase.example

import com.sumologic.shellbase.{ShellCommand, ShellCommandSet}
import org.apache.commons.cli.CommandLine

import scala.util.Random

class RNGCommandSet extends ShellCommandSet("rng", "Random Number Generator utilities") {

  private abstract class RandomXCommand(desc: String) extends ShellCommand(desc, s"Generates a random $desc") {
    protected def generator(random: Random): Any

    override def execute(cmdLine: CommandLine): Boolean = {
      println(generator(Random))

      true
    }
  }

  commands += new RandomXCommand("boolean") {
    override protected def generator(random: Random): Any = random.nextBoolean()
  }

  commands += new RandomXCommand("double") {
    override protected def generator(random: Random): Any = random.nextDouble()
  }

  commands += new RandomXCommand("float") {
    override protected def generator(random: Random): Any = random.nextFloat()
  }

  commands += new RandomXCommand("gaussian") {
    override protected def generator(random: Random): Any = random.nextGaussian()
  }

  commands += new RandomXCommand("int") {
    override protected def generator(random: Random): Any = random.nextInt()
  }

  commands += new RandomXCommand("long") {
    override protected def generator(random: Random): Any = random.nextLong()
  }

}
