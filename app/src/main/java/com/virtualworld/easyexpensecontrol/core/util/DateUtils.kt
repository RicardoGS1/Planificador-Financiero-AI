package com.virtualworld.easyexpensecontrol.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun convertTimestampToString(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

/** Devuelve el inicio del día (00:00:00) en milisegundos para la fecha del timestamp. */
fun getStartOfDay(epochMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Últimos 3 días: etiqueta corta y start-of-day en ms, del más reciente al más antiguo. */
fun getLastThreeDays(todayLabel: String, yesterdayLabel: String): List<Pair<String, Long>> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    cal.add(Calendar.DAY_OF_MONTH, -1)
    val yesterdayStart = cal.timeInMillis
    cal.add(Calendar.DAY_OF_MONTH, -1)
    val dayBeforeStart = cal.timeInMillis
    val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
    return listOf(
        todayLabel to todayStart,
        yesterdayLabel to yesterdayStart,
        formatter.format(Date(dayBeforeStart)) to dayBeforeStart
    )
}

/** Fin del día (23:59:59.999) en ms para un start-of-day. */
fun getEndOfDay(startOfDayMs: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = startOfDayMs
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

/** Lista de los últimos N días: (etiqueta, startMs). Del más reciente al más antiguo. */
fun getLastNDays(n: Int, todayLabel: String, yesterdayLabel: String): List<Pair<String, Long>> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
    return (0 until n).map { i ->
        val start = cal.timeInMillis
        val label = when (i) {
            0 -> todayLabel
            1 -> yesterdayLabel
            else -> formatter.format(Date(start))
        }
        cal.add(Calendar.DAY_OF_MONTH, -1)
        label to start
    }
}

/** Inicio del mes (día 1, 00:00:00) para el mes/año dados. */
fun getStartOfMonth(year: Int, month: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Fin del mes (último día 23:59:59.999). */
fun getEndOfMonth(year: Int, month: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

/** Inicio del año (1 enero 00:00:00). */
fun getStartOfYear(year: Int): Long {
    return getStartOfMonth(year, 1)
}

/** Fin del año (31 diciembre 23:59:59.999). */
fun getEndOfYear(year: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, Calendar.DECEMBER)
    cal.set(Calendar.DAY_OF_MONTH, 31)
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

