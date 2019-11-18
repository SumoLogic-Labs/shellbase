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

import com.sumologic.shellbase.CommonWordSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NotificationCommandSetTest extends CommonWordSpec {
  "Notification Command Set" should {
    "list notifications" in {
      val manager = new InMemoryShellNotificationManager("", Seq(createNotification("test")))
      val sut = new NotificationCommandSet(manager)
      sut.executeLine(List("list"))
    }

    "list notifications (even if empty)" in {
      val manager = new InMemoryShellNotificationManager("", Seq.empty)
      val sut = new NotificationCommandSet(manager)
      sut.executeLine(List("list"))
    }

    "let you toggle on/off all notifications at once" in {
      val manager = new InMemoryShellNotificationManager("", Seq(createNotification("1"), createNotification("2"), createNotification("3")))
      val sut = new NotificationCommandSet(manager)

      sut.executeLine(List("enable"))
      manager.notificationEnabled("1") should be(true)
      manager.notificationEnabled("2") should be(true)
      manager.notificationEnabled("3") should be(true)

      sut.executeLine(List("disable"))
      manager.notificationEnabled("1") should be(false)
      manager.notificationEnabled("2") should be(false)
      manager.notificationEnabled("3") should be(false)

      sut.executeLine(List("enable", "all"))
      manager.notificationEnabled("1") should be(true)
      manager.notificationEnabled("2") should be(true)
      manager.notificationEnabled("3") should be(true)

      sut.executeLine(List("disable", "all"))
      manager.notificationEnabled("1") should be(false)
      manager.notificationEnabled("2") should be(false)
      manager.notificationEnabled("3") should be(false)
    }

    "let you toggle on/off notifications individually/in a group" in {
      val manager = new InMemoryShellNotificationManager("", Seq(createNotification("1"), createNotification("2"), createNotification("3")))
      val sut = new NotificationCommandSet(manager)

      sut.executeLine(List("enable", "1"))
      manager.notificationEnabled("1") should be(true)
      manager.notificationEnabled("2") should be(false)
      manager.notificationEnabled("3") should be(false)

      sut.executeLine(List("disable", "1"))
      manager.notificationEnabled("1") should be(false)
      manager.notificationEnabled("2") should be(false)
      manager.notificationEnabled("3") should be(false)

      sut.executeLine(List("enable", "2,3"))
      manager.notificationEnabled("1") should be(false)
      manager.notificationEnabled("2") should be(true)
      manager.notificationEnabled("3") should be(true)

      sut.executeLine(List("disable", "1,3"))
      manager.notificationEnabled("1") should be(false)
      manager.notificationEnabled("2") should be(true)
      manager.notificationEnabled("3") should be(false)

    }
  }

  private def createNotification(n: String) = new ShellNotification {
    override def notify(title: String, message: String): Unit = ???

    override def name: String = n
  }
}
