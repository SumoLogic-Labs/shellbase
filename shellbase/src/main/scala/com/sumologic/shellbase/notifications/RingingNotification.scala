package com.sumologic.shellbase.notifications

class RingingNotification extends ShellNotification {
  override def name: String = "bell"

  override def notify(message: String): Unit = {
    (0 to 10).foreach {
      i => {
        System.out.print("\u0007")
        Thread.sleep(100)
      }
    }
    System.out.println()
  }
}
