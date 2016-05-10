package com.sumologic.shellbase.notifications

trait ShellNotificationManager {

  def notifierNames: Seq[String]

  def notificationEnabled(notification: String): Boolean

  def notify(message: String): Unit

  def enable(notifier: String): Unit

  def disable(notifier: String): Unit

}
