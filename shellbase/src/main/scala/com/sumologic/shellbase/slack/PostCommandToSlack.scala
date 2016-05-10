package com.sumologic.shellbase.slack

import com.flyberrycapital.slack.SlackClient
import com.sumologic.shellbase.ShellCommand

/**
  * This enables automatically posting the command executed to Slack to document it.
  */
trait PostCommandToSlack extends ShellCommand with PostToSlackHelper {

  override def postCommandToSlack(commandPath: List[String], arguments: List[String]): Option[String] = {
    try {
      retry(maxAttempts = 3, sleepTime = 1000) {
        for (msg <- slackMessage(commandPath, arguments)) {
          sendSlackMessageIfConfigured(s"[$username] $msg")
        }
        None
      }
    } catch {
      case e: Exception => {
        val msg = s"unable to log to slack channel `${slackState.slackChannel.getOrElse("unknown")}`"
        _logger.warn(msg, e)
        Some(msg)
      }
    }
  }

  protected def slackMessage(commandPath: List[String], arguments: List[String]): Option[String] = {
    val cmdPath = commandPath.mkString(" ").trim
    val args = arguments.mkString(" ").trim

    if (args.isEmpty) {
      Some(s"`$cmdPath`")
    } else {
      Some(s"`$cmdPath $args`")
    }
  }

  private def retry[T](maxAttempts: Int, sleepTime: Long)(f: => T): T = {
    try {
      f
    } catch {
      case e: Exception =>
        if (maxAttempts == 1) {
          throw e
        } else {
          if (sleepTime > 0) {
            Thread.sleep(sleepTime)
          }
          retry(maxAttempts - 1, sleepTime)(f)
        }
    }
  }
}

/**
  * This provides utility to ShellCommands to post anything to Slack as part of the command
  */
trait PostToSlackHelper {
  protected val slackState: SlackState

  protected val username: String = System.getProperty("user.name", "unknown")
  protected val blacklistedUsernames: Set[String] = Set.empty

  def slackMessagingConfigured = slackState.slackClient.isDefined && slackState.slackChannel.isDefined

  def sendSlackMessageIfConfigured(msg: String, additionalOptions: Map[String, String] = Map.empty): Unit = {
    if (!blacklistedUsernames.contains(username)) {
      for (client <- slackState.slackClient;
           channel <- slackState.slackChannel) {
        client.chat.postMessage(channel, msg, slackState.slackOptions ++ additionalOptions)
      }
    }
  }
}

trait SlackState {
  def slackClient: Option[SlackClient]

  def slackChannel: Option[String]

  def slackOptions: Map[String, String] = Map()
}
