package com.virtualworld.easyexpensecontrol.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun convertTimestampToString(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
