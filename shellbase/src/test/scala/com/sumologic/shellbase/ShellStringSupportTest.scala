package com.sumologic.shellbase

import com.sumologic.shellbase.ShellStringSupport._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ShellStringSupportTest extends CommonWordSpec {
  "ShellStringSupport" should {
    "calculate the visible length of a string without escapes" in {
      val s = "a string"
      s.visibleLength should be(8)
    }

    "calculate the visible length of a string with escapes" in {
      val s = ShellColors.red("a red string")
      s.visibleLength should be(12)
    }

    "trim a string with escapes" in {
      val s = s"a ${ShellColors.red("red")} string"
      s.escapedTrim(6) should be(s"a ${ShellColors.red("red")} ")
    }
  }
}
