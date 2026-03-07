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

/** Últimos 3 días: etiqueta corta (ej. "Hoy", "Ayer", "15/02") y start-of-day en ms, del más reciente al más antiguo. */
fun getLastThreeDays(): List<Pair<String, Long>> {
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
        "Hoy" to todayStart,
        "Ayer" to yesterdayStart,
        formatter.format(Date(dayBeforeStart)) to dayBeforeStart
    )
}
