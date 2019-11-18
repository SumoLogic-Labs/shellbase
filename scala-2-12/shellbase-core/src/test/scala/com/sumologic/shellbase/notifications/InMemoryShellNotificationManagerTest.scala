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
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[JUnitRunner])
class InMemoryShellNotificationManagerTest extends CommonWordSpec with BeforeAndAfterEach with MockitoSugar {

  "InMemoryShellNotificationManager" should {
    "provide notification names" in {
      val sut = new InMemoryShellNotificationManager("", Seq(notification1, notification2))
      sut.notifierNames should be(Seq(firstName, secondName))
    }

    "know if a notification is enabled by default" in {
      val sut = new InMemoryShellNotificationManager("", Seq(notification1, notification2), enabledByDefault = false)
      sut.notificationEnabled(firstName) should be(false)
      sut.notificationEnabled(secondName) should be(false)
      sut.notificationEnabled("madeUp") should be(false)

      val sut2 = new InMemoryShellNotificationManager("", Seq(notification1, notification2), enabledByDefault = true)
      sut2.notificationEnabled(firstName) should be(true)
      sut2.notificationEnabled(secondName) should be(true)
      sut2.notificationEnabled("madeUp") should be(true)
    }

    "support enabling and disabling notifications" in {
      val sut = new InMemoryShellNotificationManager("", Seq(notification1, notification2))
      sut.notificationEnabled(firstName) should be(false)
      sut.notificationEnabled(secondName) should be(false)

      sut.enable(firstName)
      sut.notificationEnabled(firstName) should be(true)
      sut.notificationEnabled(secondName) should be(false)

      sut.enable(secondName)
      sut.notificationEnabled(firstName) should be(true)
      sut.notificationEnabled(secondName) should be(true)

      sut.disable(firstName)
      sut.notificationEnabled(firstName) should be(false)
      sut.notificationEnabled(secondName) should be(true)

      sut.disable(secondName)
      sut.notificationEnabled(firstName) should be(false)
      sut.notificationEnabled(secondName) should be(false)
    }

    "only notify enabled notifications" in {
      val notificationString = "test"
      val sut = new InMemoryShellNotificationManager("", Seq(notification1, notification2))

      sut.notify(notificationString)
      verify(notification1, times(0)).notify("", notificationString)
      verify(notification2, times(0)).notify("", notificationString)

      sut.enable(firstName)
      sut.notify(notificationString)
      verify(notification1, times(1)).notify("", notificationString)
      verify(notification2, times(0)).notify("", notificationString)

      sut.enable(secondName)
      sut.notify(notificationString)
      verify(notification1, times(2)).notify("", notificationString)
      verify(notification2, times(1)).notify("", notificationString)

      sut.disable(firstName)
      sut.notify(notificationString)
      verify(notification1, times(2)).notify("", notificationString)
      verify(notification2, times(2)).notify("", notificationString)

    }
  }

  private val firstName = "first"
  private val secondName = "second"

  private var notification1: ShellNotification = _
  private var notification2: ShellNotification = _

  override protected def beforeEach(): Unit = {
    notification1 = mock[ShellNotification]
    notification2 = mock[ShellNotification]

    when(notification1.name).thenReturn(firstName)
    when(notification2.name).thenReturn(secondName)
  }
}
