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
