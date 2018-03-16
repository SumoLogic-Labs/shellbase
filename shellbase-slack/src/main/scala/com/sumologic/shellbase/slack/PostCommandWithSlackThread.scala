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

trait PostCommandWithSlackThread extends PostCommandToSlack {

  override def postCommandToSlack(commandPath: List[String], arguments: List[String]): Option[String] = {
    try {
      var tsOpt: Option[String] = None
      retry(maxAttempts = 3, sleepTime = 1000) {
        for (msg <- slackMessage(commandPath, arguments)) {
          sendSlackMessageIfConfigured(s"[$username] $msg") match {
            case Some(postMessageResponse) => tsOpt = Some(postMessageResponse.ts)
            case None => None
          }
        }
        tsOpt
      }
    } catch {
      case e: Exception => {
        val msg = s"unable to log to slack channel `${slackState.slackChannel.getOrElse("unknown")}`"
        _logger.warn(msg, e)
        None
      }
    }
  }

  override def postInformationToSlackThread(ts: String, commandExecuteTimeDuration: Long, commandResult: Boolean):
  Option[String] = {
    val replyMessage = {
      if (commandResult) {
        // we use 5 minutes as a time threshold that we want to be notified by Slack about command execution result
        if (commandExecuteTimeDuration >= 5) s"Hey @${username}， command successed"
        else "success"
      } else {
        if (commandExecuteTimeDuration >= 5) "Hey @${username}， command failed"
        else "failure"
      }
    }
    val linkName = {
      if (commandExecuteTimeDuration >= 5) Map("link_names" -> "1")
      else Map.empty
    }
    val threadInfo = Map("thread_ts" -> ts)

    try {
      retry(maxAttempts = 3, sleepTime = 1000) {
        sendSlackMessageIfConfigured(replyMessage, linkName ++ threadInfo)
      }
      None
    } catch {
      case e: Exception => {
        val msg = s"unable to log to slack channel `${slackState.slackChannel.getOrElse("unknown")}`"
        _logger.warn(msg, e)
        None
      }
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
