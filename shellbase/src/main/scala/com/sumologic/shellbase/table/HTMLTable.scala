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
