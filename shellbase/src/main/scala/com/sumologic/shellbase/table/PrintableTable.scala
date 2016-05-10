package com.sumologic.shellbase.table

abstract class PrintableTable[T] {

  protected case class Column(
                               heading: String,
                               maxWidth: Int,
                               value: T => String,
                               rightAligned: Boolean)

  var trimValues = true

  var bare = true

  var emptyCell: String = "---"

  final def addColumn(
                       heading: String,
                       maxWidth: Int,
                       value: T => String,
                       rightAligned: Boolean = false): Unit = {
    require(maxWidth > 0, "Max width must be positive!")
    columns = columns :+ Column(heading, maxWidth, value, rightAligned)
  }

  def renderLines(rows: Seq[T]): Seq[String]

  protected var columns = List[Column]()
}
