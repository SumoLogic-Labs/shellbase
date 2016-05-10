package com.sumologic.shellbase.notifications

trait ShellNotification {
  def name: String

  def notify(message: String): Unit
}
