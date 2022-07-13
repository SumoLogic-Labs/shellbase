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

import com.sumologic.shellbase.ShellCommand

/**
  * This enables automatically posting the command executed to Slack to document it.
  */
trait PostCommandToSlack extends ShellCommand with PostToSlackHelper {

  override def postCommandToSlack(commandPath: List[String], arguments: List[String],
                                  comments: Option[String]): Option[String] = {
    try {
      retry(maxAttempts = 3, sleepTime = 1000) {
        for (msg <- slackMessage(commandPath, arguments, comments)) {
          sendSlackMessageIfConfigured(s"[$username] $msg")
        }
        None
      }
    } catch {
      case e: Exception => {
        val msg = s"unable to log to slack channel `${slackState.slackChannels}`"
        _logger.warn(msg, e)
        Some(msg)
      }
    }
  }

  // VisibleForTesting
  protected[slack] def slackMessage(commandPath: List[String], arguments: List[String],
                             comments: Option[String]): Option[String] = {
    val cmdPath = commandPath.mkString(" ").trim
    val args = arguments.mkString(" ").trim

    Some(List(
      Some("`"),
      Some(cmdPath),
      if (args.isEmpty) None else Some(s" $args"),
      Some("`"),
      comments.map(c => s"\n$c")
    ).flatten.mkString(""))
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




