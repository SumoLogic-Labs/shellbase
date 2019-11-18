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
package com.sumologic.shellbase.timeutil

import org.joda.time.format.{PeriodFormatter, PeriodFormatterBuilder}
import org.joda.time.{Period, PeriodType}
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

object TimeFormats {

  private val _logger = LoggerFactory.getLogger(getClass)

  private val tersePeriodFormatter = new PeriodFormatterBuilder().
    appendYears().appendSuffix("y").
    appendDays().appendSuffix("d").
    appendHours().appendSuffix("h").
    appendMinutes().appendSuffix("m").
    appendSeconds().appendSuffix("s").
    toFormatter

  private val compactPeriodFormatterWithMs = new PeriodFormatterBuilder().
    appendDays().appendSuffix("d ").
    appendHours().appendSuffix("h ").
    appendMinutes().appendSuffix("m ").
    appendSeconds().appendSuffix("s ").
    appendMillis().appendSuffix("ms ").
    toFormatter

  val errorString = "<error>"

  private def format(formatter: PeriodFormatter, period: Long) = {
    formatter.print(new Period(period, PeriodType.yearDayTime()).normalizedStandard(PeriodType.dayTime()))
  }

  def formatWithMillis(period: Long): String = if (period == 0) "0" else format(compactPeriodFormatterWithMs, period).trim

  def formatAsTersePeriod(period: Long): String = {
    try {
      if (period == 0) {
        "0"
      } else if (period < 1000) {
        formatWithMillis(period)
      } else {
        format(tersePeriodFormatter, period)
      }
    } catch {
      case NonFatal(e) =>
        _logger.error(s"Failed to parse period: $period", e)
        errorString
    }
  }

  def parseTersePeriod(period: String): Option[Long] = {
    if (period == null || period.trim.isEmpty) {
      return None
    }

    try {
      Some(tersePeriodFormatter.parsePeriod(period).toStandardDuration.getMillis)
    } catch {
      case iae: IllegalArgumentException => {
        try {
          Some(period.toLong)
        } catch {
          case nfe: NumberFormatException => None
        }
      }
    }
  }

}
