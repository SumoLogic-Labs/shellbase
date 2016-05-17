package com.sumologic.shellbase.slack

import com.flyberrycapital.slack.Methods.Chat
import com.flyberrycapital.slack.SlackClient
import com.sumologic.shellbase.ShellCommand
import org.apache.commons.cli.CommandLine
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatest.junit.JUnitRunner
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => matcher_eq, _}
import org.scalatest.mock.MockitoSugar

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
      val (_, chatClient) = createMockClient
      val channel = createAChannel
      sut.postCommandToSlack(List("Hi"), List.empty) should be (None)

      verify(chatClient, times(1)).postMessage(matcher_eq(channel), anyString(), anyObject[Map[String, String]]())
    }

    "return an exception as text when thrown" in {
      val (slackClient, _) = createMockClient
      val channel = createAChannel

      when(slackClient.chat).thenThrow(new RuntimeException)

      sut.postCommandToSlack(List.empty, List.empty) should be ('defined)
    }

    "retry and maybe eventually succeed" in {
      val (slackClient, chatClient) = createMockClient
      val channel = createAChannel

      when(slackClient.chat).thenThrow(new RuntimeException).thenReturn(chatClient)

      sut.postCommandToSlack(List.empty, List.empty) should be (None)
      verify(chatClient, times(1)).postMessage(matcher_eq(channel), anyString(), anyObject[Map[String, String]]())

    }
  }

  private def createMockClient: (SlackClient, Chat) = {
    val client = mock[SlackClient]
    val chat = mock[Chat]
    when(mockState.slackClient).thenReturn(Some(client))
    when(client.chat).thenReturn(chat)
    (client, chat)
  }

  private def createAChannel: String = {
    val channel = "#my_channel"
    when(mockState.slackChannel).thenReturn(Some(channel))
    channel
  }

  var sut: PostCommandToSlack = _
  var mockState: SlackState = _

  override protected def beforeEach(): Unit = {
    mockState = mock[SlackState]

    when(mockState.slackClient).thenReturn(None)
    when(mockState.slackChannel).thenReturn(None)
    when(mockState.slackOptions).thenReturn(Map.empty[String, String])

    sut = new ShellCommand("", "") with PostCommandToSlack {
      override protected val slackState: SlackState = mockState
      override def execute(cmdLine: CommandLine): Boolean = ???
    }
  }

}
