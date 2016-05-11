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

object ShellColors extends ShellFormattingHelper {
  def black: String => String = format(Console.BLACK)(_)

  def blue: String => String = format(Console.BLUE)(_)

  def cyan: String => String = format(Console.CYAN)(_)

  def green: String => String = format(Console.GREEN)(_)

  def magenta: String => String = format(Console.MAGENTA)(_)

  def red: String => String = format(Console.RED)(_)

  def white: String => String = format(Console.WHITE)(_)

  def yellow: String => String = format(Console.YELLOW)(_)
}


object ShellHighlights extends ShellFormattingHelper {
  def black: String => String = format(Console.BLACK_B)(_)

  def blue: String => String = format(Console.BLUE_B)(_)

  def cyan: String => String = format(Console.CYAN_B)(_)

  def green: String => String = format(Console.GREEN_B)(_)

  def magenta: String => String = format(Console.MAGENTA_B)(_)

  def red: String => String = format(Console.RED_B)(_)

  def white: String => String = format(Console.WHITE_B)(_)

  def yellow: String => String = format(Console.YELLOW_B)(_)
}


object ShellFormatting extends ShellFormattingHelper {
  def blink: String => String = format(Console.BLINK)(_)

  def bold: String => String = format(Console.BOLD)(_)

  def invisible: String => String = format(Console.INVISIBLE)(_)

  def reversed: String => String = format(Console.REVERSED)(_)

  def underlined: String => String = format(Console.UNDERLINED)(_)
}

object ShellStringSupport {

  implicit class StringOps(val self: String) extends AnyVal {
    def escapeCodeLength: Int = AnsiEscape.findAllMatchIn(self).map(m => m.end - m.start).sum

    def visibleLength: Int = self.length - self.escapeCodeLength

    def escapedTrim(maxLength: Int): String = {
      val escapePositions = AnsiEscape.findAllMatchIn(self).map(m => (m.start, m.end)).toSeq
      val actualMaxLength = actualTrimLength(maxLength, escapePositions)
      if (actualMaxLength > self.length) self else self.substring(0, actualMaxLength)
    }

    def escapedTrimWithReset(maxLength: Int): String = escapedTrim(maxLength) + Console.RESET

    private def actualTrimLength(acc: Int, escapePositions: Seq[(Int, Int)]): Int = {
      escapePositions match {
        case (start, end) +: tail =>
          if (acc < start) acc else actualTrimLength(acc + end - start, tail)
        case _ => acc
      }
    }
  }

  val AnsiEscape = "\u001b\\[\\d{1,2}m".r
}


private[shellbase] trait ShellFormattingHelper {
  protected def format(formatType: String)(string: String): String = {
    formatType + string + Console.RESET
  }
}
