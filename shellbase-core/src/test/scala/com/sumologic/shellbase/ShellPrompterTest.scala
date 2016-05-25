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

import jline.console.ConsoleReader
import org.junit.runner.RunWith
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
      sut.readChar("blah", Seq.empty) should be ('a'.toInt)
      sut.readChar("blah", Seq.empty) should be ('b'.toInt)
      sut.readChar("blah", Seq.empty) should be ('c'.toInt)
    }

    "accept a subset of characters if specified" in {
      feedCharacters("abc")
      sut.readChar("blah", Seq('a', 'c')) should be ('a'.toInt)
      sut.readChar("blah", Seq('a', 'c')) should be ('c'.toInt)
    }
  }

  private def feedCharacters(string: String): Unit = {
    queue.enqueue(string.toCharArray: _*)
  }

  private def feedCharacters(string: Char*): Unit = {
    queue.enqueue(string: _*)
  }

  private var reader: ConsoleReader = _
  private var sut: ShellPrompter = _
  //  private var stream: InputStream = _
  private var queue: mutable.Queue[Char] = _

  override protected def beforeEach(): Unit = {
    queue = new mutable.Queue[Char]()
    val stream = new InputStream() {
      override def read(): Int = queue.dequeue().toInt
    }
    reader = new ConsoleReader(stream, System.out)
    sut = new ShellPrompter(in = reader)
  }

}
