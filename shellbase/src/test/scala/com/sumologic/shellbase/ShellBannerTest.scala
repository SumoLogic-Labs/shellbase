package com.sumologic.shellbase

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ShellBannerTest extends CommonWordSpec {
  "ShellBanner" should {
    "load banners from resources" in {
      println("Eyeball it:")
      println(ShellBanner.Warning)

      ShellBanner.Warning.contains("_///////") should be(true)
    }
  }
}
