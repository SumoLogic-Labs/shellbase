package com.sumologic.shellbase.cmdline

import com.sumologic.shellbase.CommonWordSpec
import com.sumologic.shellbase.cmdline.RichCommandLine._
import org.apache.commons.cli.Options
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RichCommandLineTest extends CommonWordSpec {

  "Command line options" should {
    "return a default value if no value was provided on the command line" in {

      val defaultValue = "blargh"
      val sut = new CommandLineOption("s", "test", true, "halp", Some(defaultValue))

      val options = new Options
      options += sut

      val cmdLine = Array[String]().parseCommandLine(options)
      cmdLine.get.get(sut).get should equal(defaultValue)
    }

    "return a provided value if one was provided despite a default" in {

      val defaultValue = "blargh"
      val sut = new CommandLineOption("s", "test", true, "halp", Some(defaultValue))

      val options = new Options
      options += sut

      val providedValue = "wtf?"
      val cmdLine = Array[String]("-s", providedValue).parseCommandLine(options)
      cmdLine.get.get(sut).get should equal(providedValue)
    }

    "not accept options with the same short name" in {
      val options = new Options
      options += new CommandLineOption("s", "one", true, "same shit")
      the[IllegalArgumentException] thrownBy {
        options += new CommandLineOption("s", "two", true, "different day")
      }
    }

    "not accept options with the same long name" in {
      val options = new Options
      options += new CommandLineOption("x", "same", true, "same shit")
      the[IllegalArgumentException] thrownBy {
        options += new CommandLineOption("y", "same", true, "different day")
      }
    }
  }

}
