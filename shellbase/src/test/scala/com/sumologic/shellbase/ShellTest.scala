package com.sumologic.shellbase

import jline.console.ConsoleReader
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ShellTest extends CommonWordSpec with MockitoSugar {
  "ShellPrompter" should {
    "validate questions" in {
      val input = mock[ConsoleReader]
      val question = "Do you like testing?"
      val answer = "answer"

      when(input.readLine(contains(question), isNull(classOf[Character]))).thenReturn(answer)

      val prompter = new ShellPrompter(input)
      prompter.askQuestion(question, List(ShellPromptValidators.nonEmpty)) should equal(answer)
    }
  }

  "ShellPromptValidators" should {
    "throw exception for invalid options for chooseNCommaSeparated" in {
      intercept[IllegalArgumentException] {
        ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 4)("1,2")
      }
    }

    "validate as success for valid result for chooseNCommaSeparated" in {
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1,2").valid shouldBe true
    }

    "validate as failure for invalid result for chooseNCommaSeparated" in {
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1").valid shouldBe false
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1,4").valid shouldBe false
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1,4,5").valid shouldBe false
    }

    "accept spaces around commas in chooseNCommaSeparated" in {
      ShellPromptValidators.chooseNCommaSeparated(Seq("1", "2", "3"), 2)("1, 2").valid shouldBe true
    }
  }
}
