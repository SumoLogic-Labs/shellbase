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


trait CommandNamingConvention {
  def nameVersions(inputName: String): Iterable[String]
  def sanitizedName(input: String): String
  def validateName(input: String): Boolean
}

class EmptyCommandNamingConvention() {
  def nameVersions(inputName: String): Iterable[String] = List(inputName)
  def sanitizedName(input: String): String = input
  def validateName(input: String): Boolean = true
}

class SeparatorNamingConvention(validSeparator: Char, disapprovedSeparators: List[Char])
  extends CommandNamingConvention {

  def nameVersions(inputName: String): Iterable[String] = {
    val separators = (disapprovedSeparators :+ validSeparator).toSet
    val separatorPairs = separators.flatMap(
      x => separators.flatMap(y => if (x != y) Some(x, y) else None))
    val replacedPairs = separatorPairs.map(x => inputName.replace(x._1, x._2))
    (replacedPairs.toList ::: List(inputName, inputName.filterNot(separators))).distinct
  }

  def sanitizedName(input: String): String = {
    input.map(c => if (disapprovedSeparators.contains(c)) validSeparator else c)
  }

  def validateName(input: String): Boolean = {
    def checkChar(char: Char): Boolean = input.contains(char)
    !disapprovedSeparators.map(checkChar).reduce(_ || _)
  }
}
