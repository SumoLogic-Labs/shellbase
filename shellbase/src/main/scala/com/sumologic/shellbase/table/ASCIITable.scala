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
package com.sumologic.shellbase.table

import com.sumologic.shellbase.ShellStringSupport._

class ASCIITable[T](ansiEscapeSupport: Boolean = false) extends PrintableTable[T] {
  override def renderLines(rows: Seq[T]): Seq[String] = {
    renderLines(rows, None)
  }

  /**
    * ASCIITable line rendering method with column filter. The filter is based on column indexes.
    * Column index is defined in table column schema.
    *
    * @param rows                array of input table row object
    * @param filterColumnIndexes column filter set
    * @return array of output
    */
  def renderLines(rows: Seq[T], filterColumnIndexes: Option[Set[Int]]): Seq[String] = {
    require(columns.nonEmpty, "No columns specified!")
    require(rows.nonEmpty, "No data!")

    val renderedRows = rows.map(renderRow)
    val indices = 0 to columns.size

    val linePrefix = if (bare) " " else "| "
    val lineSuffix = if (bare) " " else " |"

    val rawCellFormats = columns.zip(indices).map {
      tpl =>
        val maxColumnWidth = renderedRows.map(r => strlen(r(tpl._2))).max.max(strlen(tpl._1.heading))
        val safeMaxWidth = tpl._1.maxWidth.min(maxColumnWidth)
        CellFormat(safeMaxWidth, tpl._1.rightAligned)
    }

    def filterByColumnIndexesIfNeeded[K](fields: List[K]) = {
      if (filterColumnIndexes.isDefined) {
        fields.zipWithIndex.filter { c => filterColumnIndexes.get.contains(c._2) }.map {
          _._1
        }
      } else {
        fields
      }
    }

    val cellFormats = filterByColumnIndexesIfNeeded(rawCellFormats)

    val lineFormat = LineFormat(linePrefix, lineSuffix, cellFormats)
    val widths = cellFormats.map(_.maxWidth)

    val separatorOuter = if (bare) "" else "+"

    val separator = separatorOuter + widths.map(_ + 1).map(width => (0 to width).map(i => '-').mkString).mkString("+") + separatorOuter
    val headLine = lineFormat.render(filterByColumnIndexesIfNeeded(columns).map(_.heading))
    val dataLines = renderedRows.map { r => filterByColumnIndexesIfNeeded(r) }.map(r => lineFormat.render(r))

    if (bare) {
      List(headLine, separator.trim) ++ dataLines
    } else {
      List(separator, headLine, separator) ++ dataLines ++ List(separator)
    }
  }

  private val strlen: String => Int = if (ansiEscapeSupport) _.visibleLength else _.length

  private def trim(s: String, maxLength: Int): String = {
    if (ansiEscapeSupport) {
      s.escapedTrimWithReset(maxLength)
    } else {
      if (maxLength >= s.length) s else s.substring(0, maxLength)
    }
  }

  private def trimmed(widths: Seq[Int], strings: Seq[String]): Seq[String] = {
    if (trimValues) {
      widths.zip(strings).map(tpl => trim(tpl._2, tpl._1))
    } else {
      strings
    }
  }

  private def renderRow(row: T): List[String] = {
    columns.map(c => Option(c.value(row)).getOrElse(emptyCell))
  }


  private case class CellFormat(maxWidth: Int, rightAligned: Boolean) {
    def render(content: String): String = {
      val (left, right) = {
        val spaceCount = (maxWidth - strlen(content)).max(0)
        val spaces = List.fill(spaceCount)(" ").mkString
        if (rightAligned) (spaces, "") else ("", spaces)
      }
      s"$left$content$right"
    }
  }

  private case class LineFormat(linePrefix: String, lineSuffix: String, cells: Seq[CellFormat]) {
    def render(values: Seq[String]): String = {
      val cellsWithContent = cells.zip(trimmed(cells.map(_.maxWidth), values))
      linePrefix + cellsWithContent.map(cwc => cwc._1.render(cwc._2)).mkString(" | ") + lineSuffix
    }
  }

}
