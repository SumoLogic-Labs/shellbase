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

import slack.models.{Attachment, Block}

/**
  * This provides utility to ShellCommands to post anything to Slack as part of the command
  */
trait PostToSlackHelper {
  protected val slackState: SlackState

  protected def username: String = System.getProperty("user.name", "unknown")
  protected val excludedUsernames: Set[String] = Set.empty

  def slackMessagingConfigured: Boolean = slackState.slackClient.isDefined && slackState.slackChannels.nonEmpty

  def slackChannelFilter(channelName: String): Boolean = true

  // NOTE: Normally we don't want to expose external dependencies classes as part of an interface, but both Attachment
  // and Block are very well defined and very isolated.  So while package may change if we need to fork, it can be
  // handled as a single-line replacement.
  def sendSlackMessageIfConfigured(msg: String,
                                   additionalOptions: Map[String, String] = Map.empty,
                                   attachments: Option[Seq[Attachment]] = None,
                                   blocks: Option[Seq[Block]] = None
                                  ): Option[String] = {
    require(!additionalOptions.contains("attachments"),
      "`attachments` is a separate argument and can't be specified as part of additionalOptions")

    require(!additionalOptions.contains("blocks"),
      "`blocks` is a separate argument and can't be specified as part of additionalOptions")

    var message: Option[String] = None
    if (!excludedUsernames.contains(username)) {
      for (client <- slackState.slackClient;
           channel <- slackState.slackChannels.filter(slackChannelFilter)) {
        val username: Option[String] = additionalOptions.get("username")
          .orElse(Some(slackState.userNameToBeUsedWhenPosting))
        val asUser: Option[Boolean] = additionalOptions.get("as_user").map(_.toBoolean)
        val parse: Option[String] = additionalOptions.get("parse")
        val linkNames: Option[String] = additionalOptions.get("link_names")
        val unfurlLinks: Option[Boolean] = additionalOptions.get("unfurl_links").map(_.toBoolean)
        val unfurlMedia: Option[Boolean] = additionalOptions.get("unfurl_media").map(_.toBoolean)
        val iconUrl: Option[String] = additionalOptions.get("icon_url")
        val iconEmoji: Option[String] = additionalOptions.get("icon_emoji")
        val replaceOriginal: Option[Boolean] = additionalOptions.get("replace_original").map(_.toBoolean)
        val deleteOriginal: Option[Boolean] = additionalOptions.get("delete_original").map(_.toBoolean)
        val threadTs: Option[String] = additionalOptions.get("thread_ts")
        val replyBroadcast: Option[Boolean] = additionalOptions.get("reply_broadcast").map(_.toBoolean)


        message = Some(client.postChatMessage(channelId = channel, text = msg, username = username, asUser = asUser,
          parse = parse, linkNames = linkNames, attachments = attachments, blocks = blocks, unfurlLinks = unfurlLinks,
          unfurlMedia = unfurlMedia, iconUrl = iconUrl, iconEmoji = iconEmoji, replaceOriginal = replaceOriginal,
          deleteOriginal = deleteOriginal, threadTs = threadTs, replyBroadcast = replyBroadcast)(slackState.actorSystem))
      }
    }

    message
  }
}
