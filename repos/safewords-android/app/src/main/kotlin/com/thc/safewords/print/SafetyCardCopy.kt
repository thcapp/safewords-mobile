package com.thc.safewords.print

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * Loads /shared/safety-card-copy.json from the app's assets so card text stays
 * consistent with iOS. Strings include {groupName} placeholders that the
 * caller must fill in.
 */
object SafetyCardCopy {

    private var cached: JsonObject? = null

    fun load(context: Context): JsonObject {
        cached?.let { return it }
        val json = context.assets.open("safety-card-copy.json").bufferedReader().use { it.readText() }
        val root = Gson().fromJson(json, JsonObject::class.java)
        cached = root
        return root
    }

    fun card(context: Context, key: String): JsonObject =
        load(context).getAsJsonObject("cards").getAsJsonObject(key)

    fun str(json: JsonObject, key: String, vars: Map<String, String> = emptyMap()): String {
        var s = json.get(key)?.asString ?: ""
        vars.forEach { (k, v) -> s = s.replace("{$k}", v) }
        return s
    }

    fun strList(json: JsonObject, key: String): List<String> =
        json.getAsJsonArray(key)?.map { it.asString } ?: emptyList()
}
