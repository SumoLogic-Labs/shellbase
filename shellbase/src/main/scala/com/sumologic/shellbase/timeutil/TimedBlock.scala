package com.sumologic.shellbase.timeutil

object TimedBlock {

  private def now = System.currentTimeMillis()

  def apply[T](name: String, printer: String => Unit = println(_))(func: => T) = {

    val start = now

    def timeTaken = TimeFormats.formatWithMillis(now - start)

    printer(s"START $name")
    try {
      val res = func
      printer(s"DONE  $name (took: $timeTaken)")
      res
    } catch {
      case e: Exception => {
        printer(s"FAIL  $name (after: $timeTaken)")
        throw e
      }
    }
  }
}
