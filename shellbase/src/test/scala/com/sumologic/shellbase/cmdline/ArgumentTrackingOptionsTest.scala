package com.sumologic.shellbase.cmdline

import com.sumologic.shellbase.CommonWordSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ArgumentTrackingOptionsTest extends CommonWordSpec {
  "ArgumentTrackingOptions" should {
    "correctly convert arguments list" in {
      val sut = new ArgumentTrackingOptions
      val item = new CommandLineArgument("a", 1, false)
      sut.addArgument(item)
      sut.getArguments should contain(item)
    }
  }

}
