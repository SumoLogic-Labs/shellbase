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

import com.flyberrycapital.slack.Responses.PostMessageResponse

/**
  * This provides utility to ShellCommands to post anything to Slack as part of the command
  */
trait PostToSlackHelper {
  protected val slackState: SlackState

  protected val username: String = System.getProperty("user.name", "unknown")
  protected val blacklistedUsernames: Set[String] = Set.empty

  def slackMessagingConfigured: Boolean = slackState.slackClient.isDefined && slackState.slackChannels.nonEmpty

  def sendSlackMessageIfConfigured(msg: String, additionalOptions: Map[String, String] = Map.empty):
  Option[PostMessageResponse] = {
    var message : Option[PostMessageResponse] = None
    if (!blacklistedUsernames.contains(username)) {
      for (client <- slackState.slackClient;
           channel <- slackState.slackChannels) {
        message = Option(client.chat.postMessage(channel, msg, slackState.slackOptions ++ additionalOptions))
      }
    }
    message
  }
}
