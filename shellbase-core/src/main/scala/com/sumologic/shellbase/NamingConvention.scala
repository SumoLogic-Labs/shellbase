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
  def nameVersions(input: String): Iterable[String]
  def sanitizedName(input: String): String
  def validateName(input: String): Boolean
}

class SimpleCommandNamingConvention() {
  def nameVersions(input: String): Iterable[String] = List(input)
  def sanitizedName(input: String): String = input
  def validateName(input: String): Boolean = true
}

class SeparatorNamingConvention(canonicalSeparator: String, noncanonicalSeparators: List[String])
  extends CommandNamingConvention {

  def nameVersions(input: String): Iterable[String] = {
    def getNoncanonicalVersion(separator: String) = sanitizedName(input).replace(canonicalSeparator, separator)
    val noncanonicalVersions = noncanonicalSeparators.map(getNoncanonicalVersion)
    (noncanonicalVersions :+ input :+ sanitizedName(input)).distinct
  }

  def nonemptyNoncanonicalSeparators() = noncanonicalSeparators.filter(_ != "")

  def sanitizedName(input: String): String = {
    val separator = nonemptyNoncanonicalSeparators.find(s => input.contains(s))
    separator.map(s => input.replace(s, canonicalSeparator)).getOrElse(input)
  }

  def validateName(input: String): Boolean = {
    !nonemptyNoncanonicalSeparators.map(input.contains).reduce(_ || _)
  }
}
