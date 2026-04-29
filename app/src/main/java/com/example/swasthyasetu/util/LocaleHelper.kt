package com.example.swasthyasetu.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "swasthyasetu_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val DEFAULT_LANGUAGE = "en"

    val SUPPORTED_LANGUAGES = arrayOf("en", "hi")

    fun setLocale(context: Context, languageCode: String): Boolean {
        // Always persist the preference so getSavedLanguage works everywhere
        saveLanguage(context, languageCode)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ — Use the Jetpack per-app language API
            val localeList = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(localeList)
            false  // framework restarts activities automatically
        } else {
            // Pre-Android 13 — manual Configuration update
            updateResources(context, languageCode)
            true   // caller must recreate() the Activity
        }
    }

    fun getSavedLanguage(context: Context): String {
        // On API 33+ check the framework first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val appLocales = AppCompatDelegate.getApplicationLocales()
            if (!appLocales.isEmpty) {
                val tag = appLocales.toLanguageTags().split(",").firstOrNull()
                if (!tag.isNullOrBlank()) return tag
            }
        }
        // Fallback to SharedPreferences
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun onAttach(baseContext: Context): Context {
        val lang = getSavedLanguage(baseContext)
        return updateResources(baseContext, lang)
    }

    private fun saveLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // API 24+ — use LocaleList for proper fallback chain
            config.setLocales(LocaleList(locale))
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}
