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

import java.util

import jline.console.completer.Completer

/**
  * A Completer that completes based on a recursively defined List of Completers.
  *
  * Note that
  * NestedCompleter("a", NestedCompleter("b", NestedCompleter("c"))
  * is different than
  * ArgumentCompleter("a", ArgumentCompleter("b", ArgumentCompleter("c"))).
  *
  * The `ArgumentCompleter` version will stop suggest completions for "c" after typing "a b", since
  * every argument of `ArgumentCompleter` maps to at most one token (in fact, it won't even suggest
  * "b"). The `NestedCompleter` version will auto-complete for every stage of "a b c" as expected.
  */
class NestedCompleter(val head: Completer, val tail: Completer) extends Completer {

  override def complete(buffer: String, cursor: Int, candidates: util.List[CharSequence]): Int = {
    val (initWhitespace, leftTrimmedBuffer) = buffer.span(Character.isWhitespace)
    val (firstToken, remaining) = leftTrimmedBuffer.span(!Character.isWhitespace(_))

    val headCandidates = new util.LinkedList[CharSequence]()
    val headRes = head.complete(firstToken, firstToken.length min cursor, headCandidates)

    val preRemainingLength = initWhitespace.length + firstToken.length

    if (headRes == -1 || headCandidates.isEmpty) {
      -1
    } else if (cursor <= preRemainingLength) {
      candidates.addAll(headCandidates)
      initWhitespace.length + headRes
    } else {
      val newCursor = cursor - preRemainingLength
      val tailRes = tail.complete(remaining, newCursor, candidates)
      preRemainingLength + tailRes
    }
  }
}
