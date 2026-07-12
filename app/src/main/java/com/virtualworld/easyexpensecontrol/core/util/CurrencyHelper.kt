package com.virtualworld.easyexpensecontrol.core.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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

    /**
     * Símbolos canónicos ampliamente reconocidos. Evita prefijos de desambiguación
     * como US$, CA$ o MX$ que devuelve [Currency.getSymbol] fuera del país emisor.
     */
    private val canonicalSymbols: Map<String, String> = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "CNY" to "¥",
        "KRW" to "₩",
        "INR" to "₹",
        "RUB" to "₽",
        "TRY" to "₺",
        "ILS" to "₪",
        "THB" to "฿",
        "VND" to "₫",
        "PHP" to "₱",
        "UAH" to "₴",
        "PLN" to "zł",
        "BRL" to "R$",
        "MXN" to "$",
        "ARS" to "$",
        "CLP" to "$",
        "COP" to "$",
        "PEN" to "S/",
        "CHF" to "Fr.",
        "SEK" to "kr",
        "NOK" to "kr",
        "DKK" to "kr",
        "CZK" to "Kč",
        "HUF" to "Ft",
        "RON" to "lei",
        "BGN" to "лв",
        "HRK" to "kn",
        "ISK" to "kr",
        "AUD" to "$",
        "NZD" to "$",
        "CAD" to "$",
        "SGD" to "S$",
        "TWD" to "NT$",
        "ZAR" to "R",
        "AED" to "د.إ",
        "SAR" to "﷼",
        "EGP" to "£",
        "NGN" to "₦",
        "PKR" to "₨",
        "IDR" to "Rp",
        "MYR" to "RM",
        "HKD" to "HK$"
    )

    /**
     * Locale de referencia para el nombre en el idioma original de cada moneda.
     */
    private val currencyPrimaryLocaleTags: Map<String, String> = mapOf(
        "EUR" to "de_DE",
        "USD" to "en_US",
        "GBP" to "en_GB",
        "CHF" to "de_CH",
        "CAD" to "en_CA",
        "AUD" to "en_AU",
        "NZD" to "en_NZ",
        "JPY" to "ja_JP",
        "CNY" to "zh_CN",
        "KRW" to "ko_KR",
        "INR" to "hi_IN",
        "RUB" to "ru_RU",
        "TRY" to "tr_TR",
        "SAR" to "ar_SA",
        "AED" to "ar_AE",
        "ILS" to "he_IL",
        "THB" to "th_TH",
        "VND" to "vi_VN",
        "IDR" to "id_ID",
        "MXN" to "es_MX",
        "BRL" to "pt_BR",
        "ARS" to "es_AR",
        "CLP" to "es_CL",
        "COP" to "es_CO",
        "PEN" to "es_PE",
        "PLN" to "pl_PL",
        "SEK" to "sv_SE",
        "NOK" to "nb_NO",
        "DKK" to "da_DK",
        "CZK" to "cs_CZ",
        "HUF" to "hu_HU",
        "RON" to "ro_RO",
        "UAH" to "uk_UA",
        "XAF" to "fr_CM",
        "XOF" to "fr_SN",
        "XCD" to "en_AG",
        "XPF" to "fr_PF",
        "PHP" to "fil_PH",
        "TWD" to "zh_TW",
        "ZAR" to "en_ZA",
        "EGP" to "ar_EG",
        "NGN" to "en_NG",
        "PKR" to "ur_PK",
        "MYR" to "ms_MY",
        "HKD" to "zh_HK",
        "SGD" to "en_SG"
    )

    /** Países emisores prioritarios para resolver el símbolo nativo sin prefijos. */
    private val symbolCountryPriority: List<String> = listOf(
        "US", "GB", "DE", "FR", "JP", "CN", "KR", "IN", "RU", "TR",
        "SA", "AE", "IL", "TH", "VN", "ID", "MX", "BR", "AR", "CL",
        "CO", "PE", "PL", "SE", "NO", "DK", "CZ", "HU", "RO", "UA",
        "AU", "NZ", "CA", "CH", "ZA", "EG", "NG", "PK", "MY", "HK", "SG", "PH", "TW"
    )

    private val displayNameLocaleCache = ConcurrentHashMap<String, Locale>()
    private val symbolLocaleCache = ConcurrentHashMap<String, Locale>()

    fun getSavedCurrencyCode(context: Context): String {
        val preferences = prefs(context)
        if (!preferences.contains(KEY_CURRENCY_CODE)) {
            val detectedCode = detectCurrencyForDevice(context)
            preferences.edit().putString(KEY_CURRENCY_CODE, detectedCode).apply()
            return detectedCode
        }
        val saved = preferences.getString(KEY_CURRENCY_CODE, DEFAULT_CURRENCY_CODE)
            ?: DEFAULT_CURRENCY_CODE
        return saved.takeIf { isValidCurrencyCode(it) } ?: DEFAULT_CURRENCY_CODE
    }

    fun setCurrencyCode(context: Context, code: String) {
        require(isValidCurrencyCode(code)) { "Invalid currency code: $code" }
        prefs(context).edit().putString(KEY_CURRENCY_CODE, code).apply()
    }

    fun getDisplaySymbol(context: Context, currencyCode: String = getSavedCurrencyCode(context)): String {
        return symbolForCode(currencyCode)
    }

    fun getDisplayName(context: Context, currencyCode: String = getSavedCurrencyCode(context)): String {
        return displayNameForCode(currencyCode)
    }

    fun getAllCurrencies(context: Context): List<CurrencyInfo> {
        return Currency.getAvailableCurrencies()
            .map { currency -> currencyInfoForCode(currency.currencyCode) }
            .distinctBy { it.code }
            .sortedWith(compareBy({ it.displayName }, { it.code }))
    }

    fun currencyInfo(context: Context, code: String = getSavedCurrencyCode(context)): CurrencyInfo {
        return currencyInfoForCode(code)
    }

    private fun currencyInfoForCode(code: String): CurrencyInfo {
        return CurrencyInfo(
            code = code,
            displayName = displayNameForCode(code),
            symbol = symbolForCode(code)
        )
    }

    private fun displayNameForCode(code: String): String {
        return runCatching {
            Currency.getInstance(code).getDisplayName(localeForDisplayName(code))
        }.getOrDefault(code)
    }

    private fun symbolForCode(code: String): String {
        canonicalSymbols[code]?.let { return it }

        val locale = localeForSymbol(code)
        val defaultCurrencyCode = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
        if (defaultCurrencyCode == code) {
            val symbol = Currency.getInstance(code).getSymbol(locale).trim()
            if (symbol.isNotEmpty() && !symbol.equals(code, ignoreCase = true)) {
                val cleaned = cleanDisambiguatedSymbol(symbol, code)
                if (cleaned.isNotEmpty()) return cleaned
            }
        }

        return code
    }

    /**
     * Elimina prefijos de país en símbolos desambiguados (p. ej. US$ → $, CA$ → $).
     * Solo aplica cuando el prefijo coincide con el código ISO de la moneda.
     */
    private fun cleanDisambiguatedSymbol(symbol: String, code: String): String {
        val prefix = code.take(2)
        if (symbol.length > 2 &&
            symbol.startsWith(prefix, ignoreCase = true) &&
            symbol.drop(2).startsWith("$")
        ) {
            return symbol.drop(2)
        }
        if (symbol.length > 3 &&
            symbol.startsWith(code, ignoreCase = true)
        ) {
            return symbol.drop(code.length).trim().ifEmpty { symbol }
        }
        return symbol
    }

    private fun localeForDisplayName(code: String): Locale {
        displayNameLocaleCache[code]?.let { return it }

        currencyPrimaryLocaleTags[code]?.let { tag ->
            val locale = Locale.forLanguageTag(tag)
            displayNameLocaleCache[code] = locale
            return locale
        }

        val locale = Locale.getAvailableLocales()
            .asSequence()
            .filter { it.country.isNotEmpty() }
            .filter { candidate ->
                runCatching { Currency.getInstance(candidate).currencyCode == code }.getOrDefault(false)
            }
            .sortedWith(
                compareBy<Locale> { if (it.language == "en") 1 else 0 }
                    .thenBy { it.country }
            )
            .firstOrNull()
            ?: Locale.getDefault()

        displayNameLocaleCache[code] = locale
        return locale
    }

    private fun localeForSymbol(code: String): Locale {
        symbolLocaleCache[code]?.let { return it }

        currencyPrimaryLocaleTags[code]?.let { tag ->
            val locale = Locale.forLanguageTag(tag)
            symbolLocaleCache[code] = locale
            return locale
        }

        val candidates = Locale.getAvailableLocales()
            .asSequence()
            .filter { it.country.isNotEmpty() }
            .filter { candidate ->
                runCatching { Currency.getInstance(candidate).currencyCode == code }.getOrDefault(false)
            }
            .toList()

        val locale = candidates
            .sortedWith(
                compareBy<Locale>(
                    { locale -> symbolCountryPriority.indexOf(locale.country).let { if (it == -1) Int.MAX_VALUE else it } },
                    { it.country }
                )
            )
            .firstOrNull()
            ?: Locale.getDefault()

        symbolLocaleCache[code] = locale
        return locale
    }

    private fun detectCurrencyForDevice(context: Context): String {
        val configuration = context.resources.configuration
        val locales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            buildList {
                for (index in 0 until configuration.locales.size()) {
                    add(configuration.locales[index])
                }
            }
        } else {
            @Suppress("DEPRECATION")
            listOf(configuration.locale)
        }

        for (locale in locales) {
            val code = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
            if (code != null && isValidCurrencyCode(code)) {
                return code
            }
        }

        val fallbackLocale = locales.firstOrNull() ?: Locale.getDefault()
        val languageTag = fallbackLocale.toLanguageTag()
        val countryTag = fallbackLocale.country.takeIf { it.isNotEmpty() }?.let { country ->
            "${fallbackLocale.language}_$country"
        }
        listOfNotNull(countryTag, languageTag)
            .mapNotNull { tag ->
                runCatching { Currency.getInstance(Locale.forLanguageTag(tag)).currencyCode }.getOrNull()
            }
            .firstOrNull { isValidCurrencyCode(it) }
            ?.let { return it }

        return DEFAULT_CURRENCY_CODE
    }

    private fun isValidCurrencyCode(code: String): Boolean =
        code.length == 3 && runCatching { Currency.getInstance(code) }.isSuccess

    private fun prefs(context: Context): SharedPreferences {
        val base = context.applicationContext ?: context
        return base.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
