package com.virtualworld.easyexpensecontrol.core.util

import java.time.LocalDate as JavaLocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

object RecurringDateHelper {

    fun yearMonthFromMillis(dateMillis: Long): Int {
        val date = Instant.fromEpochMilliseconds(dateMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        return date.year * 100 + date.monthNumber
    }

    fun dayOfMonthFromMillis(dateMillis: Long): Int =
        Instant.fromEpochMilliseconds(dateMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date.dayOfMonth

    fun nextYearMonth(yearMonth: Int): Int {
        val year = yearMonth / 100
        val month = yearMonth % 100
        return if (month == 12) (year + 1) * 100 + 1 else year * 100 + (month + 1)
    }

    fun previousYearMonth(yearMonth: Int): Int {
        val year = yearMonth / 100
        val month = yearMonth % 100
        return if (month == 1) (year - 1) * 100 + 12 else year * 100 + (month - 1)
    }

    fun lastProcessedYearMonthOnReactivation(dayOfMonth: Int): Int {
        val nextScheduled = nextScheduledDate(dayOfMonth)
        return previousYearMonth(nextScheduled.year * 100 + nextScheduled.monthNumber)
    }

    fun daysInMonth(year: Int, month: Int): Int =
        JavaLocalDate.of(year, month, 1).lengthOfMonth()

    fun millisAtStartOfDay(year: Int, month: Int, day: Int): Long =
        LocalDate(year, month, day)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

    fun localDateFromMillis(dateMillis: Long): LocalDate =
        Instant.fromEpochMilliseconds(dateMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

    fun nextScheduledDate(dayOfMonth: Int, fromDate: LocalDate = localDateFromMillis(System.currentTimeMillis())): LocalDate {
        val year = fromDate.year
        val month = fromDate.monthNumber
        val day = minOf(dayOfMonth, daysInMonth(year, month))
        val candidateThisMonth = LocalDate(year, month, day)
        if (candidateThisMonth >= fromDate) return candidateThisMonth

        val nextYearMonth = nextYearMonth(year * 100 + month)
        val nextYear = nextYearMonth / 100
        val nextMonth = nextYearMonth % 100
        val nextDay = minOf(dayOfMonth, daysInMonth(nextYear, nextMonth))
        return LocalDate(nextYear, nextMonth, nextDay)
    }
}
