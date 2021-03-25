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
package com.sumologic.shellbase.slack

import akka.actor.ActorSystem
import com.sumologic.shellbase.ShellCommand
import org.apache.commons.cli.CommandLine
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.junit.JUnitRunner
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => matcher_eq, _}
import org.scalatestplus.mockito.MockitoSugar
import slack.api.BlockingSlackApiClient

@RunWith(classOf[JUnitRunner])
class PostCommandToSlackTest extends CommonWordSpec with BeforeAndAfterEach with MockitoSugar {

  "PostCommandToSlack" should {
    "do nothing when no client or channel is provided" in {
      sut.postCommandToSlack(List.empty, List.empty) should be (None)
      sut.slackMessagingConfigured should be (false)
    }

    "do nothing with a client but no channel" in {
      createMockClient
      sut.postCommandToSlack(List.empty, List.empty) should be (None)
      sut.slackMessagingConfigured should be (false)
    }

    "do nothing with a channel but no client" in {
      createAChannel
      sut.postCommandToSlack(List.empty, List.empty) should be (None)
      sut.slackMessagingConfigured should be (false)
    }

    "send the message when given correct setup" in {
      val slackClient = createMockClient
      val channel = createAChannel
      sut.slackMessagingConfigured should be (true)

      sut.postCommandToSlack(List("Hi"), List.empty) should be (None)
      verifyCallToPost(slackClient, times(1), channel)

      sut.postCommandToSlack(List("Hi"), List("with", "params")) should be (None)
      verifyCallToPost(slackClient, times(2), channel)
    }

    "send the message when multiple channels configured" in {
      val slackClient = createMockClient
      val channels = createTwoChannels
      sut.slackMessagingConfigured should be (true)

      sut.postCommandToSlack(List("Hi"), List.empty) should be (None)
      channels.foreach(channel =>
        verifyCallToPost(slackClient, times(1), channel)
      )

      sut.postCommandToSlack(List("Hi"), List("with", "params")) should be (None)
      channels.foreach(channel =>
        verifyCallToPost(slackClient, times(2), channel)
      )
    }

    "not send the message to filtered out channels when configured" in {
      val slackClient = createMockClient
      val channel = "#my_channel3"
      createTwoChannelsAndOneExcluded
      sut.slackMessagingConfigured should be (true)

      sut.postCommandToSlack(List("Hi"), List.empty) should be (None)
      verifyCallToPost(slackClient, times(0), channel)

      sut.postCommandToSlack(List("Hi"), List("with", "params")) should be (None)
      verifyCallToPost(slackClient, times(0), channel)
    }

    "return an exception as text when thrown" in {
      val slackClient = createMockClient
      val channel = createAChannel
      sut.slackMessagingConfigured should be (true)

      whenPostingToAnyChannel(slackClient).thenThrow(new RuntimeException)

      sut.postCommandToSlack(List.empty, List.empty) should be ('defined)
    }

    "retry and maybe eventually succeed" in {
      val slackClient = createMockClient
      val channel = createAChannel
      sut.slackMessagingConfigured should be (true)

      whenPostingToAnyChannel(slackClient).thenThrow(new RuntimeException).thenReturn("hi")

      sut.postCommandToSlack(List.empty, List.empty) should be (None)
      verifyCallToPost(slackClient, times(2), channel)
    }
  }

  private def createMockClient: BlockingSlackApiClient = {
    val client = mock[BlockingSlackApiClient]
    when(mockState.slackClient).thenReturn(Some(client))
    client
  }

  private def createAChannel: String = {
    val channel = "#my_channel"
    when(mockState.slackChannel).thenReturn(Some(channel))
    when(mockState.slackChannels).thenReturn(Some(channel).toList)
    channel
  }

  private def createTwoChannels: List[String] = {
    val channels = List("#my_channel1", "#my_channel2")
    when(mockState.slackChannels).thenReturn(channels)
    channels
  }

  private def createTwoChannelsAndOneExcluded: List[String] = {
    val channels = List("#my_channel1", "#my_channel2", "#my_channel3")
    when(mockState.slackChannels).thenReturn(channels)
    channels
  }

  var sut: PostCommandToSlack = _
  var mockState: SlackState = _

  override protected def beforeEach(): Unit = {
    mockState = mock[SlackState]

    when(mockState.slackClient).thenReturn(None)
    when(mockState.slackChannel).thenReturn(None)
    when(mockState.slackChannels).thenReturn(None.toList)
    when(mockState.slackOptions).thenReturn(Map.empty[String, String])
    when(mockState.actorSystem).thenReturn(mock[ActorSystem])

    sut = new ShellCommand("", "") with PostCommandToSlack {
      override protected val slackState: SlackState = mockState
      override def slackChannelFilter(channelName: String): Boolean = channelName != "#my_channel3"
      override def execute(cmdLine: CommandLine): Boolean = ???
    }
  }

}
