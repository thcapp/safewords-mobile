package com.thc.safewords.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thc.safewords.SafewordsApp

/**
 * Frozen word lists for TOTP derivation.
 *
 * These lists are part of the algorithm — changing them would cause the same
 * seed to produce different words. They are loaded from JSON assets bundled
 * with the app and cached in memory.
 *
 * - 197 adjectives
 * - 300 nouns
 */
object WordLists {

    val adjectives: List<String> by lazy { loadWordList("wordlists/adjectives.json") }
    val nouns: List<String> by lazy { loadWordList("wordlists/nouns.json") }

    private fun loadWordList(assetPath: String): List<String> {
        val context: Context = SafewordsApp.instance
        val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }
}
