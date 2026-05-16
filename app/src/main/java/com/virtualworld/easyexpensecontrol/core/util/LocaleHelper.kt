package com.virtualworld.easyexpensecontrol.core.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Gestiona el idioma seleccionado por el usuario y lo aplica al [Context] que reciben las
 * Activities/Application antes de inflar recursos. La preferencia se guarda en
 * SharedPreferences propias del helper para no acoplar el resto de la app.
 *
 * Idiomas soportados: ver [SUPPORTED_LANGUAGE_TAGS]. Una etiqueta vacía o nula significa
 * "seguir el idioma del sistema".
 */
object LocaleHelper {

    private const val PREFS_NAME = "app_locale_prefs"
    private const val KEY_LANGUAGE_TAG = "selected_language_tag"

    /** Etiquetas BCP-47 de los idiomas que ofrece la app (la cadena vacía representa "sistema"). */
    val SUPPORTED_LANGUAGE_TAGS: List<String> = listOf("", "en", "es", "de", "hi", "ru")

    fun getSavedLanguageTag(context: Context): String {
        return prefs(context).getString(KEY_LANGUAGE_TAG, "") ?: ""
    }

    fun setLanguageTag(context: Context, tag: String) {
        prefs(context).edit().putString(KEY_LANGUAGE_TAG, tag).apply()
    }

    /**
     * Devuelve un [Context] con la [Configuration] forzada al idioma guardado.
     * Si la etiqueta está vacía, devuelve el contexto original (idioma del sistema).
     */
    fun applySavedLocale(context: Context): Context {
        val tag = getSavedLanguageTag(context)
        if (tag.isBlank()) return context
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLayoutDirection(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return context.createConfigurationContext(config)
    }

    private fun prefs(context: Context): SharedPreferences {
        // OJO: durante Application.attachBaseContext, applicationContext aún es null,
        // por eso usamos el propio context recibido (que ya admite SharedPreferences).
        val base = context.applicationContext ?: context
        return base.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
