package uk.co.milncj.quick.data

import android.content.Context
import org.json.JSONArray

class CategoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, 0)

    fun load(): List<String> {
        val raw = prefs.getString(KEY_CATEGORIES, null) ?: return QuickCategories.defaults
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    json.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }.distinctBy { it.lowercase() }.ifEmpty { QuickCategories.defaults }
        }.getOrElse { QuickCategories.defaults }
    }

    fun save(categories: List<String>) {
        val clean = categories
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(MAX_CATEGORIES)
        val json = JSONArray()
        clean.forEach(json::put)
        prefs.edit().putString(KEY_CATEGORIES, json.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "quick_preferences"
        private const val KEY_CATEGORIES = "categories"
        const val MAX_CATEGORIES = 12
    }
}
