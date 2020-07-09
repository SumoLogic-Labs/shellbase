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
import com.sumologic.shellbase.ShellCommand
import com.sumologic.shellbase.slack.{CommonWordSpec, PostCommandWithSlackThread, SlackState}

import com.flyberrycapital.slack.Methods.Chat
import com.flyberrycapital.slack.Responses.PostMessageResponse
import com.flyberrycapital.slack.SlackClient
import org.apache.commons.cli.CommandLine
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyObject, anyString}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Matchers.{eq => matcher_eq, _}

@RunWith(classOf[JUnitRunner])
class PostCommandWithSlackThreadTest extends CommonWordSpec with BeforeAndAfterEach with MockitoSugar {

  "PostCommandWithSlackThreads" should {
    "do nothing when no client or channel is provided" in {
      sut.postCommandToSlack(List.empty, List.empty) should be(None)
      sut.slackMessagingConfigured should be(false)
    }

    "do nothing with a client but no channel" in {
      createMockClient
      sut.postCommandToSlack(List.empty, List.empty) should be(None)
      sut.slackMessagingConfigured should be(false)
    }

    "do nothing with a channel but no client" in {
      createAChannel
      sut.postCommandToSlack(List.empty, List.empty) should be(None)
      sut.slackMessagingConfigured should be(false)
    }

    "send the message when given correct setup" in {
      val (_, chatClient) = createMockClient
      val channel = createAChannel
      sut.slackMessagingConfigured should be(true)

      sut.postCommandToSlack(List("Hi"), List.empty) should be(Some(ts))
      verify(chatClient, times(1)).postMessage(matcher_eq(channel), anyString(), anyObject[Map[String, String]]())

      sut.postCommandToSlack(List("Hi"), List("with", "params")) should be(Some(ts))
      verify(chatClient, times(2)).postMessage(matcher_eq(channel), anyString(), anyObject[Map[String, String]]())
    }

    "send the message when multiple channels configured" in {
      val (_, chatClient) = createMockClient
      val channels = createTwoChannels
      sut.slackMessagingConfigured should be (true)

      sut.postCommandToSlack(List("Hi"), List.empty) should be (Some(ts))
      channels.foreach(channel =>
        verify(chatClient, times(1)).postMessage(matcher_eq(channel), anyString(), anyObject[Map[String, String]]())
      )

      sut.postCommandToSlack(List("Hi"), List("with", "params")) should be (Some(ts))
      channels.foreach(channel =>
        verify(chatClient, times(2)).postMessage(matcher_eq(channel), anyString(), anyObject[Map[String, String]]())
      )
    }

    "not send send the message to filtered out channels" in {
      val (_, chatClient) = createMockClient
      createTwoChannelsAndOneBlacklistedOne
      sut.slackMessagingConfigured should be (true)

      sut.postCommandToSlack(List("Hi"), List.empty) should be (Some(ts))
      verify(chatClient, times(0)).postMessage(matcher_eq("#my_channel3"), anyString(), anyObject[Map[String, String]]())

      sut.postCommandToSlack(List("Hi"), List("with", "params")) should be (Some(ts))
      verify(chatClient, times(0)).postMessage(matcher_eq("#my_channel3"), anyString(), anyObject[Map[String, String]]())
    }

    "return an exception as text when thrown" in {
      val (slackClient, _) = createMockClient
      val channel = createAChannel
      sut.slackMessagingConfigured should be(true)

      when(slackClient.chat).thenThrow(new RuntimeException)

      sut.postCommandToSlack(List.empty, List.empty) should be(None)
    }

    "retry and maybe eventually succeed" in {
      val (slackClient, chatClient) = createMockClient
      val channel = createAChannel
      sut.slackMessagingConfigured should be(true)

      when(slackClient.chat).thenThrow(new RuntimeException).thenReturn(chatClient)

      sut.postCommandToSlack(List.empty, List.empty) should be(Some(ts))
      verify(chatClient, times(1)).postMessage(matcher_eq(channel), anyString(), anyObject[Map[String, String]]())
    }

    "reply the message in thread when given correct setup" in {
      val (slackClient, chatClient) = createMockClient
      val channel = createAChannel
      sut.slackMessagingConfigured should be(true)

      sut.postInformationToSlackThread(ts, 5, true) should be(None)
      verify(chatClient, times(1)).postMessage(matcher_eq(channel), anyString(), anyObject[Map[String, String]]())
    }

    "retry to reply message in thread and maybe eventually succeed" in {
      val (slackClient, chatClient) = createMockClient
      val channel = createAChannel
      sut.slackMessagingConfigured should be(true)

      when(slackClient.chat).thenThrow(new RuntimeException).thenReturn(chatClient)

      sut.postInformationToSlackThread(ts, 5, true) should be(None)
      verify(chatClient, times(1)).postMessage(matcher_eq(channel), anyString(), anyObject[Map[String, String]]())
    }
  }

  private def createMockClient: (SlackClient, Chat) = {
    val client = mock[SlackClient]
    val chat = mock[Chat]
    val response = mock[PostMessageResponse]
    when(mockState.slackClient).thenReturn(Some(client))
    when(client.chat).thenReturn(chat)
    when(chat.postMessage(any(), anyString(), anyObject[Map[String, String]]())).thenReturn(response)
    when(response.ts).thenReturn(ts)
    (client, chat)
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

  private def createTwoChannelsAndOneBlacklistedOne: List[String] = {
    val channels = List("#my_channel1", "#my_channel2", "#my_channel3")
    when(mockState.slackChannels).thenReturn(channels)
    channels
  }

  var sut: PostCommandWithSlackThread = _
  var mockState: SlackState = _
  val ts: String = "#slack_thread_ts"

  override protected def beforeEach(): Unit = {
    mockState = mock[SlackState]

    when(mockState.slackClient).thenReturn(None)
    when(mockState.slackChannel).thenReturn(None)
    when(mockState.slackChannels).thenReturn(None.toList)
    when(mockState.slackOptions).thenReturn(new SlackState {
      override def slackClient: Option[SlackClient] = ???

      override def slackChannel: Option[String] = ???
    }.slackOptions)

    sut = new ShellCommand("", "") with PostCommandWithSlackThread {
      override protected val slackState: SlackState = mockState

      override def slackChannelFilter(channelName: String): Boolean = channelName != "#my_channel3"

      override def execute(cmdLine: CommandLine): Boolean = ???
    }
  }

}
