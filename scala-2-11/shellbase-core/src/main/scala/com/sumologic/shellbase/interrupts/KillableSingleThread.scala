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
package com.sumologic.shellbase.interrupts

import java.util.concurrent.TimeoutException

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.Try

/**
  * Encapsulates a thread that supports:
  * - waiting for the thread to complete
  * - "killing" the thread (calling interrupt(), waiting a bit, then calling stop() if needed)
  */
class KillableSingleThread[T](fn: => T) {

  private val resultPromise = Promise[T]()
  private val thread = new Thread("killable-thread") {
    override def run(): Unit = {
      resultPromise.tryComplete(Try(fn))
    }
  }

  private def interrupt(): Unit = {
    thread.interrupt()
  }

  private def stop(): Unit = {
    //noinspection ScalaDeprecation
    thread.stop()
    resultPromise.tryFailure(new ThreadDeath)
  }

  def future: Future[T] = resultPromise.future

  def start(): Unit = {
    thread.start()
  }

  def waitForCompletion(waitDuration: Duration): Boolean = {
    try {
      Await.ready(resultPromise.future, waitDuration)
      true
    } catch {
      case _: TimeoutException => false
      case _: Throwable => future.isCompleted
    }
  }

  def kill(gracePeriod: Duration): Unit = {
    interrupt()
    if (!waitForCompletion(gracePeriod)) {
      stop()
    }
  }
}
