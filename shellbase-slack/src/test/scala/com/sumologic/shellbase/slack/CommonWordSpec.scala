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

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.{eq => matcher_eq, _}
import org.mockito.Mockito.{verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.mockito.verification.VerificationMode
import org.scalatest.{Matchers, WordSpecLike}
import slack.api.BlockingSlackApiClient
import slack.models.{Attachment, Block}

trait CommonWordSpec extends WordSpecLike with Matchers {

  protected def whenPostingToChannel(slackClient: BlockingSlackApiClient, channel: String): OngoingStubbing[String] = {
    when(slackClient.postChatMessage(matcher_eq(channel), anyString(), any[Option[String]], any[Option[Boolean]],
      any[Option[String]], any[Option[String]], any[Option[Seq[Attachment]]], any[Option[Seq[Block]]],
      any[Option[Boolean]], any[Option[Boolean]], any[Option[String]], any[Option[String]], any[Option[Boolean]],
      any[Option[Boolean]], any[Option[String]], any[Option[Boolean]])(any[ActorSystem]))
  }

  protected def whenPostingToAnyChannel(slackClient: BlockingSlackApiClient): OngoingStubbing[String] = {
    when(slackClient.postChatMessage(anyString(), anyString(), any[Option[String]], any[Option[Boolean]],
      any[Option[String]], any[Option[String]], any[Option[Seq[Attachment]]], any[Option[Seq[Block]]],
      any[Option[Boolean]], any[Option[Boolean]], any[Option[String]], any[Option[String]], any[Option[Boolean]],
      any[Option[Boolean]], any[Option[String]], any[Option[Boolean]])(any[ActorSystem]))
  }

  protected def verifyCallToPost(slackClient: BlockingSlackApiClient, mode: VerificationMode, channel: String): Unit = {
    verify(slackClient, mode).postChatMessage(matcher_eq(channel), anyString(), any[Option[String]],
      any[Option[Boolean]], any[Option[String]], any[Option[String]], any[Option[Seq[Attachment]]],
      any[Option[Seq[Block]]], any[Option[Boolean]], any[Option[Boolean]], any[Option[String]], any[Option[String]],
      any[Option[Boolean]], any[Option[Boolean]], any[Option[String]], any[Option[Boolean]])(any[ActorSystem])
  }
}
