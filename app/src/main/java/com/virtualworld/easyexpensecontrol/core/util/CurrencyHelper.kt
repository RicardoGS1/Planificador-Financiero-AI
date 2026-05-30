package com.virtualworld.easyexpensecontrol.core.util

import android.content.Context
import android.content.SharedPreferences
import java.util.Currency
import java.util.Locale

/**
 * Moneda por defecto de la app (ISO 4217). Persistida en SharedPreferences, igual que [LocaleHelper].
 */
object CurrencyHelper {

    private const val PREFS_NAME = "app_currency_prefs"
    private const val KEY_CURRENCY_CODE = "selected_currency_code"

    const val DEFAULT_CURRENCY_CODE = "EUR"

    data class CurrencyInfo(
        val code: String,
        val displayName: String,
        val symbol: String
    ) {
        val pickerLabel: String
            get() = "$displayName ($code) · $symbol"
    }

    fun getSavedCurrencyCode(context: Context): String {
        val saved = prefs(context).getString(KEY_CURRENCY_CODE, DEFAULT_CURRENCY_CODE)
            ?: DEFAULT_CURRENCY_CODE
        return saved.takeIf { isValidCurrencyCode(it) } ?: DEFAULT_CURRENCY_CODE
    }

    fun setCurrencyCode(context: Context, code: String) {
        require(isValidCurrencyCode(code)) { "Invalid currency code: $code" }
        prefs(context).edit().putString(KEY_CURRENCY_CODE, code).apply()
    }

    fun getDisplaySymbol(context: Context, currencyCode: String = getSavedCurrencyCode(context)): String {
        return symbolForCode(currencyCode, displayLocale(context))
    }

    fun getDisplayName(context: Context, currencyCode: String = getSavedCurrencyCode(context)): String {
        return runCatching {
            Currency.getInstance(currencyCode).getDisplayName(displayLocale(context))
        }.getOrDefault(currencyCode)
    }

    fun getAllCurrencies(context: Context): List<CurrencyInfo> {
        val locale = displayLocale(context)
        return Currency.getAvailableCurrencies()
            .map { currency ->
                val code = currency.currencyCode
                CurrencyInfo(
                    code = code,
                    displayName = currency.getDisplayName(locale),
                    symbol = symbolForCode(code, locale)
                )
            }
            .distinctBy { it.code }
            .sortedWith(compareBy({ it.displayName }, { it.code }))
    }

    fun currencyInfo(context: Context, code: String = getSavedCurrencyCode(context)): CurrencyInfo {
        val locale = displayLocale(context)
        return CurrencyInfo(
            code = code,
            displayName = runCatching { Currency.getInstance(code).getDisplayName(locale) }
                .getOrDefault(code),
            symbol = symbolForCode(code, locale)
        )
    }

    private fun isValidCurrencyCode(code: String): Boolean =
        code.length == 3 && runCatching { Currency.getInstance(code) }.isSuccess

    private fun symbolForCode(code: String, locale: Locale): String {
        val currency = Currency.getInstance(code)
        val symbol = currency.getSymbol(locale).trim()
        return if (symbol.isEmpty() || symbol.equals(code, ignoreCase = true)) {
            code
        } else {
            symbol
        }
    }

    private fun displayLocale(context: Context): Locale {
        val tag = LocaleHelper.getSavedLanguageTag(context)
        return if (tag.isBlank()) {
            context.resources.configuration.locales[0]
        } else {
            Locale.forLanguageTag(tag)
        }
    }

    private fun prefs(context: Context): SharedPreferences {
        val base = context.applicationContext ?: context
        return base.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
