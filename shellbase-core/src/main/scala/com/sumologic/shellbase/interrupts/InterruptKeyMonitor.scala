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

import com.sumologic.shellbase.interrupts.InterruptKeyMonitor.InterruptKeyHandler
import sun.misc.{Signal, SignalHandler}

/**
  * Wrapper to watch for the interrupt key in the shell.
  */
class InterruptKeyMonitor {

  private val interruptKeyHandler = new InterruptKeyHandler()

  def init(): Unit = {
    Signal.handle(new Signal("INT"), interruptKeyHandler)
  }

  def shutdown(): Unit = {}

  def startMonitoring(interruptCallback: => Unit): Unit = {
    interruptKeyHandler.setCallback(interruptCallback)
  }

  def stopMonitoring(): Unit = {
    interruptKeyHandler.clearCallbacks()
  }

  def isMonitoring: Boolean = {
    interruptKeyHandler.hasACallback()
  }
}

object InterruptKeyMonitor {

  class InterruptKeyHandler extends SignalHandler {

    type CallbackFn = () => Unit
    private var callbackOpt: Option[CallbackFn] = None
    private var lastInterrupt = 0L

    def setCallback(interruptCallback: => Unit): Unit = {
      callbackOpt = Some(() => interruptCallback)
    }

    def clearCallbacks(): Unit = {
      callbackOpt = None
    }

    def hasACallback(): Boolean = callbackOpt.isDefined

    def now = System.currentTimeMillis()

    override def handle(sig: Signal): Unit = {
      if (callbackOpt.isDefined) {
        if (now - lastInterrupt < 1000) {
          println("Killing the shell...")
          System.exit(1)
        } else {
          println("\nPress Ctrl-C again to exit.")
          lastInterrupt = now
          callbackOpt.foreach(_.apply())
        }
      }
    }
  }

}
