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

import java.io.InputStream
import java.text.ParseException
import java.util.Date

import jline.console.ConsoleReader
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class ShellPrompterTest extends CommonWordSpec with BeforeAndAfterEach with MockitoSugar {
  "ShellPrompter.confirm" should {
    "return true for yes answers" in {
      feedCharacters("y")
      sut.confirm("blah") should be(true)

      feedCharacters("Y")
      sut.confirm("blah") should be(true)
    }

    "return false for no answers" in {
      feedCharacters("n")
      sut.confirm("blah") should be(false)

      feedCharacters("N")
      sut.confirm("blah") should be(false)
    }

    "handle bogus input until valid character" in {
      feedCharacters("fasdfkljn")
      sut.confirm("blah") should be(false)
    }
  }

  "ShellPrompter.confirmWithDefault" should {
    "return true for yes answers" in {
      feedCharacters("y")
      sut.confirmWithDefault("blah", default = false) should be(true)

      feedCharacters("Y")
      sut.confirmWithDefault("blah", default = false) should be(true)
    }

    "return false for no answers" in {
      feedCharacters("n")
      sut.confirmWithDefault("blah", default = true) should be(false)

      feedCharacters("N")
      sut.confirmWithDefault("blah", default = true) should be(false)
    }

    "return the default for line feed" in {
      feedCharacters(sut.asciiCR)
      sut.confirmWithDefault("blah", default = false) should be(false)

      feedCharacters(sut.asciiCR)
      sut.confirmWithDefault("blah", default = true) should be(true)
    }
  }

  "ShellPrompter.readChar" should {
    "accept any character if not specified" in {
      feedCharacters("abc")
      sut.readChar("blah", Seq.empty) should be('a'.toInt)
      sut.readChar("blah", Seq.empty) should be('b'.toInt)
      sut.readChar("blah", Seq.empty) should be('c'.toInt)
    }

    "accept a subset of characters if specified" in {
      feedCharacters("abc")
      sut.readChar("blah", Seq('a', 'c')) should be('a'.toInt)
      sut.readChar("blah", Seq('a', 'c')) should be('c'.toInt)
    }
  }

  "ShellPrompter.confirmWithWarning" should {
    "return true for yes answers" in {
      feedCharacters("y")
      sut.confirmWithWarning("blah") should be(true)

      feedCharacters("Y")
      sut.confirmWithWarning("blah") should be(true)
    }

    "return false for no answers" in {
      feedCharacters("n")
      sut.confirmWithWarning("blah") should be(false)

      feedCharacters("N")
      sut.confirmWithWarning("blah") should be(false)
    }

    "handle bogus input until valid character" in {
      feedCharacters("fasdfkljn")
      sut.confirmWithWarning("blah") should be(false)
    }
  }

  "ShellPrompter.askForTime" should {
    "produce date for format" in {
      when(mockReader.readLine(anyString)).thenReturn("15 Jul 22:01")

      val expectedDate = new Date()
      expectedDate.setHours(22)
      expectedDate.setMinutes(1)
      expectedDate.setMonth(6)
      expectedDate.setDate(15)
      expectedDate.setSeconds(0)
      expectedDate.setTime((expectedDate.getTime / 1000) * 1000)

      sutWithMock.askForTime("someQuestion") should be (expectedDate)
    }

    "allow custom formats + don't change year unless needed" in {
      when(mockReader.readLine(anyString)).thenReturn("15 Jul 2013 22:01")

      val expectedDate = new Date()
      expectedDate.setHours(22)
      expectedDate.setMinutes(1)
      expectedDate.setMonth(6)
      expectedDate.setDate(15)
      expectedDate.setSeconds(0)
      expectedDate.setYear(2013 - 1900)
      expectedDate.setTime((expectedDate.getTime / 1000) * 1000)

      sutWithMock.askForTime("someQuestion", "d MMM y HH:mm") should be (expectedDate)
    }

    "bubble exception for incorrect format" in {
      when(mockReader.readLine(anyString())).thenReturn("kajsdfl;kj1")
      intercept[ParseException] {
        sutWithMock.askForTime("my question")
      }
    }
  }

  "ShellPrompter.askPasswordWithConfirmation" should {
    "require a minimum length" in {
      when(mockReader.readLine(anyString, anyChar())).thenReturn("a", "b", "cc", "cc")
      sutWithMock.askPasswordWithConfirmation(minLength = 2) should be ("cc")
    }

    "return the password if they eventually match (after retries)" in {
      when(mockReader.readLine(anyString, anyChar())).thenReturn("a", "b", "c", "d", "e", "e")
      sutWithMock.askPasswordWithConfirmation() should be ("e")
    }

    "bail out after x failed tries" in {
      when(mockReader.readLine(anyString, anyChar())).thenReturn("a", "b", "c", "d", "e", "e")
      sutWithMock.askPasswordWithConfirmation(maxAttempts = 2) should be (null)

      verify(mockReader, times(4)).readLine(anyString, anyChar)
    }
  }

  "ShellPrompter.askQuestion" should {
    "validate questions" in {
      val question = "Do you like testing?"
      val answer = "answer"

      when(mockReader.readLine(contains(question), isNull(classOf[Character]))).thenReturn(answer)

      sutWithMock.askQuestion(question, List(ShellPromptValidators.nonEmpty)) should equal(answer)
    }

    "support returning a default" in {
      val question = "Do you like testing?"
      val default = "hi!"

      when(mockReader.readLine(contains(question), isNull(classOf[Character]))).thenReturn("")

      sutWithMock.askQuestion(question, List(ShellPromptValidators.nonEmpty), default = default) should equal(default)
    }
  }

  private def feedCharacters(string: String): Unit = {
    queue.enqueue(string.toCharArray: _*)
  }

  private def feedCharacters(string: Char*): Unit = {
    queue.enqueue(string: _*)
  }

  private var queue: mutable.Queue[Char] = _
  private var reader: ConsoleReader = _
  private var sut: ShellPrompter = _

  // NOTE(chris, 2016-05-26): Mocks do not work on final methods.  In those cases, we feed characters directly. Use
  // this when mocked method is not final.  (No, I'm not proud of having to do this.)
  private var mockReader: ConsoleReader = _
  private var sutWithMock: ShellPrompter = _


  override protected def beforeEach(): Unit = {
    queue = new mutable.Queue[Char]()
    val stream = new InputStream() {
      override def read(): Int = queue.dequeue().toInt
    }
    reader = new ConsoleReader(stream, System.out)
    sut = new ShellPrompter(in = reader)

    mockReader = mock[ConsoleReader]
    sutWithMock = new ShellPrompter(in = mockReader)
  }

}
