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
