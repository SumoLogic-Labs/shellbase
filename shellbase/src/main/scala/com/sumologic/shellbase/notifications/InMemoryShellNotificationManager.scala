package com.sumologic.shellbase.notifications

class InMemoryShellNotificationManager(notifications: Seq[ShellNotification], enabledByDefault: Boolean = false)
  extends ShellNotificationManager {

  private[this] var enabledMap = Map[String, Boolean]()

  override def notifierNames: Seq[String] = {
    notifications.map(_.name)
  }

  override def notificationEnabled(notification: String): Boolean = {
    enabledMap.getOrElse(notification, enabledByDefault)
  }

  override def notify(message: String): Unit = {
    notifications.filter(n => notificationEnabled(n.name)).foreach(_.notify(message))
  }

  override def enable(notifier: String): Unit = {
    enabledMap += notifier -> true
  }

  override def disable(notifier: String): Unit = {
    enabledMap += notifier -> false
  }
}
