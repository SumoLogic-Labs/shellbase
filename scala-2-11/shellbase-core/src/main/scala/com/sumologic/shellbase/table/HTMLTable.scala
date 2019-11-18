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

class HTMLTable[T](HeadingOpt: Option[String] = None) extends PrintableTable[T] {

  override def renderLines(rows: Seq[T]): Seq[String] = {
    import HTMLTable._

    require(columns.nonEmpty, "No columns specified!")
    require(rows.nonEmpty, "No data!")
    val renderedRows: Seq[Array[String]] = rows.map(renderRow)

    val tablePrefix = if (bare)
      "<table>"
    else
      "<table border=\"1\">"
    val headLineFormatString = linePrefix + columns.map {
      col =>
        s"<th>%s</th>"
    }.mkString("") + lineSuffix
    val dataFormatString = linePrefix + columns.map {
      col =>
        val align = if (col.rightAligned)
          "align=\"right\""
        else
          ""
        s"<td $align>%s</td>"
    }.mkString("") + lineSuffix
    val headline = headLineFormatString.format(columns.map(_.heading).toArray: _*)
    val dataLines = renderedRows.map(r => dataFormatString.format(r: _*))
    List(htmlPrefix) ++ List(tableHeading(HeadingOpt), tablePrefix) ++ List(headline) ++ dataLines ++ List(tableSuffix) ++ List(htmlSuffix)
  }

  private def renderRow(row: T): Array[String] = {
    columns.map(c => Option(c.value(row)).getOrElse(emptyCell)).toArray
  }
}

object HTMLTable {
  private val htmlPrefix = "<html>"
  private val htmlSuffix = "</html>"
  private val tableSuffix = "</table>"
  private val linePrefix = "<tr>"
  private val lineSuffix = "</tr>"

  private def tableHeading(description: Option[String]) = description match {
    case Some(string) => s"<h3> $string </h3>"
    case None => "<h3> </h3>"
  }
}
