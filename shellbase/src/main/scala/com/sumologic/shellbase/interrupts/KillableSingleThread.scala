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
