package com.thc.safewords.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.thc.safewords.model.RotationInterval

/**
 * v1.3 group config primitives. Mirrors /shared/primitive-vectors.json and
 * /shared/migration-vectors.json. A group can have one or more primitives
 * enabled; the call-screen UX adapts accordingly.
 *
 * Storage policy: we store config only, never derived secrets. The override
 * word and challenge/answer table are recomputed on demand from the seed.
 */
data class Primitives(
    val rotatingWord: RotatingWordPrimitive = RotatingWordPrimitive(),
    val challengeAnswer: ChallengeAnswerPrimitive = ChallengeAnswerPrimitive(),
    val staticOverride: StaticOverridePrimitive = StaticOverridePrimitive(),
)

data class RotatingWordPrimitive(
    val enabled: Boolean = true,
    val intervalSeconds: Int = 86400,
    val wordFormat: WordFormat = WordFormat.ADJECTIVE_NOUN_NUMBER,
)

data class ChallengeAnswerPrimitive(
    val enabled: Boolean = false,
    val tableVersion: Int = 1,
    val rowCount: Int = 100,
)

data class StaticOverridePrimitive(
    val enabled: Boolean = false,
    val derivationVersion: Int = 1,
    val printedAt: Long? = null,
)

enum class WordFormat(val key: String) {
    ADJECTIVE_NOUN_NUMBER("adjective_noun_number"),
    NUMERIC("numeric");

    companion object {
        fun fromKey(key: String?): WordFormat =
            entries.firstOrNull { it.key == key } ?: ADJECTIVE_NOUN_NUMBER
    }
}

/**
 * Schema-versioned group migration. Decodes any v1.2 (legacy) or v1.3 group
 * JSON into v1.3 Primitives. Used by GroupRepository on read.
 */
object GroupSchema {

    const val CURRENT_VERSION = 2

    /**
     * Migrate a parsed group JSON object to v1.3 Primitives.
     *
     * Rule: if schemaVersion < 2 OR primitives field missing, expand with
     * rotatingWord.enabled=true at the legacy interval; everything else
     * disabled. Pass-through if the group is already v1.3.
     */
    fun migrate(groupJson: JsonObject, fallbackInterval: RotationInterval): Primitives {
        val schemaVersion = groupJson.get("schemaVersion")?.asInt ?: 1
        val primitivesJson = groupJson.getAsJsonObject("primitives")

        if (schemaVersion >= CURRENT_VERSION && primitivesJson != null) {
            return parsePrimitives(primitivesJson)
        }

        val intervalSeconds = groupJson.get("intervalSeconds")?.asInt
            ?: fallbackInterval.seconds

        return Primitives(
            rotatingWord = RotatingWordPrimitive(
                enabled = true,
                intervalSeconds = intervalSeconds,
                wordFormat = WordFormat.ADJECTIVE_NOUN_NUMBER,
            ),
            challengeAnswer = ChallengeAnswerPrimitive(enabled = false),
            staticOverride = StaticOverridePrimitive(enabled = false),
        )
    }

    /**
     * Migrate a raw JSON string. Returns Primitives configured per the
     * shared migration contract.
     */
    fun migrateFromJson(json: String, fallbackInterval: RotationInterval = RotationInterval.DAILY): Primitives {
        val obj = JsonParser.parseString(json).asJsonObject
        return migrate(obj, fallbackInterval)
    }

    private fun parsePrimitives(json: JsonObject): Primitives {
        val rotating = json.getAsJsonObject("rotatingWord")
        val ca = json.getAsJsonObject("challengeAnswer")
        val override = json.getAsJsonObject("staticOverride")

        return Primitives(
            rotatingWord = if (rotating != null) RotatingWordPrimitive(
                enabled = rotating.get("enabled")?.asBoolean ?: true,
                intervalSeconds = rotating.get("intervalSeconds")?.asInt ?: 86400,
                wordFormat = WordFormat.fromKey(rotating.get("wordFormat")?.asString),
            ) else RotatingWordPrimitive(),

            challengeAnswer = if (ca != null) ChallengeAnswerPrimitive(
                enabled = ca.get("enabled")?.asBoolean ?: false,
                tableVersion = ca.get("tableVersion")?.asInt ?: 1,
                rowCount = ca.get("rowCount")?.asInt ?: 100,
            ) else ChallengeAnswerPrimitive(),

            staticOverride = if (override != null) StaticOverridePrimitive(
                enabled = override.get("enabled")?.asBoolean ?: false,
                derivationVersion = override.get("derivationVersion")?.asInt ?: 1,
                printedAt = override.get("printedAt")?.let {
                    if (it.isJsonNull) null else it.asLong
                },
            ) else StaticOverridePrimitive(),
        )
    }
}
