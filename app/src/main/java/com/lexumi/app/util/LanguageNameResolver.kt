package com.lexumi.app.util

import java.util.Locale

/**
 * Matches a language name the user typed by hand (e.g. "Англійська",
 * "Іспанська", "English") against the device's known locales, so we can
 * find a matching offline TTS voice for it automatically.
 */
object LanguageNameResolver {

    fun resolveLocale(languageName: String): Locale? {
        val query = normalize(languageName)
        if (query.isEmpty()) return null

        val baseLocales = Locale.getAvailableLocales().filter { it.country.isEmpty() && it.language.isNotEmpty() }

        // Exact match first, checked in Ukrainian, English, and the language's own native name.
        baseLocales.firstOrNull { locale ->
            query == normalize(locale.getDisplayLanguage(Locale("uk"))) ||
                query == normalize(locale.getDisplayLanguage(Locale.ENGLISH)) ||
                query == normalize(locale.getDisplayLanguage(locale))
        }?.let { return it }

        // Fall back to a loose contains-match (handles "Англ" or extra words the user typed).
        return baseLocales.firstOrNull { locale ->
            val uk = normalize(locale.getDisplayLanguage(Locale("uk")))
            uk.isNotEmpty() && (uk.contains(query) || query.contains(uk))
        }
    }

    private fun normalize(text: String): String =
        text.trim().lowercase().removeSuffix(" мова").removeSuffix(" language").trim()
}
