package com.virtualworld.easyexpensecontrol.core.util

import android.content.Context
import java.util.Locale

object CurrencyFormatter {

    fun symbol(context: Context): String = CurrencyHelper.getDisplaySymbol(context)

    fun format(context: Context, amount: Double): String =
        "%.2f %s".format(Locale.getDefault(), amount, symbol(context))

    fun formatSigned(context: Context, amount: Double, isIncome: Boolean): String {
        val prefix = if (isIncome) "+" else "-"
        return "$prefix%.2f %s".format(Locale.getDefault(), amount, symbol(context))
    }

    fun formatChartAxisValue(context: Context, value: Int): String =
        "$value${symbol(context)}"
}
