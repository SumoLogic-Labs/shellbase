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

    def now = System.currentTimeMillis()

    override def handle(sig: Signal) {
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
