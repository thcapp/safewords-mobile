package com.thc.safewords.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.thc.safewords.model.RotationInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupSchemaTest {

    private val migrations: MigrationVectorsFile by lazy {
        val text = readResource("/migration-vectors.json")
        Gson().fromJson(text, object : TypeToken<MigrationVectorsFile>() {}.type)
    }

    @Test
    fun every_migration_vector_produces_expected_primitives() {
        for (case in migrations.v1_2_to_v1_3) {
            val expected = case.output.primitives
            val actual = GroupSchema.migrate(
                groupJson = case.input,
                fallbackInterval = RotationInterval.DAILY,
            )

            assertEquals(
                "${case.name}: rotatingWord.enabled",
                expected.rotatingWord.enabled,
                actual.rotatingWord.enabled,
            )
            assertEquals(
                "${case.name}: rotatingWord.intervalSeconds",
                expected.rotatingWord.intervalSeconds,
                actual.rotatingWord.intervalSeconds,
            )
            assertEquals(
                "${case.name}: rotatingWord.wordFormat",
                expected.rotatingWord.wordFormat,
                actual.rotatingWord.wordFormat.key,
            )
            assertEquals(
                "${case.name}: challengeAnswer.enabled",
                expected.challengeAnswer.enabled,
                actual.challengeAnswer.enabled,
            )
            assertEquals(
                "${case.name}: staticOverride.enabled",
                expected.staticOverride.enabled,
                actual.staticOverride.enabled,
            )
        }
    }

    @Test
    fun missing_primitives_field_defaults_to_rotating_only() {
        val json = """
            {
              "id": "x",
              "name": "Family",
              "intervalSeconds": 86400
            }
        """.trimIndent()

        val primitives = GroupSchema.migrateFromJson(json, RotationInterval.DAILY)

        assertEquals(true, primitives.rotatingWord.enabled)
        assertEquals(86400, primitives.rotatingWord.intervalSeconds)
        assertEquals(WordFormat.ADJECTIVE_NOUN_NUMBER, primitives.rotatingWord.wordFormat)
        assertEquals(false, primitives.challengeAnswer.enabled)
        assertEquals(false, primitives.staticOverride.enabled)
        assertNull(primitives.staticOverride.printedAt)
    }

    @Test
    fun fallback_interval_used_when_intervalSeconds_missing() {
        val json = """{"id": "x", "name": "Family"}"""
        val primitives = GroupSchema.migrateFromJson(json, RotationInterval.HOURLY)
        assertEquals(3600, primitives.rotatingWord.intervalSeconds)
    }

    @Test
    fun v1_3_pass_through_preserves_numeric_format() {
        val json = """
            {
              "schemaVersion": 2,
              "id": "x",
              "name": "Family",
              "primitives": {
                "rotatingWord": { "enabled": true, "intervalSeconds": 86400, "wordFormat": "numeric" },
                "challengeAnswer": { "enabled": true, "tableVersion": 1, "rowCount": 100 },
                "staticOverride": { "enabled": true, "derivationVersion": 1, "printedAt": null }
              }
            }
        """.trimIndent()

        val primitives = GroupSchema.migrateFromJson(json, RotationInterval.DAILY)
        assertEquals(WordFormat.NUMERIC, primitives.rotatingWord.wordFormat)
        assertEquals(true, primitives.challengeAnswer.enabled)
        assertEquals(true, primitives.staticOverride.enabled)
    }

    private fun readResource(path: String): String {
        val stream = javaClass.getResourceAsStream(path)
            ?: throw IllegalStateException("Test resource $path not on classpath. Did the gradle copySharedToTestResources task run?")
        return stream.bufferedReader().use { it.readText() }
    }
}

// Pretty-printed Gson maps to camelCase fields by default; vector file uses
// "v1.2_to_v1.3" which is non-identifier — register custom mapping via
// SerializedName? Simpler: use a wrapper file that uses safe Kotlin naming
// then deserialize the v1.2_to_v1.3 array via JsonObject inspection.
private data class MigrationVectorsFile(
    val version: Int,
    @com.google.gson.annotations.SerializedName("v1.2_to_v1.3")
    val v1_2_to_v1_3: List<MigrationCase>,
)

private data class MigrationCase(
    val name: String,
    val input: JsonObject,
    val output: ExpectedOutput,
)

private data class ExpectedOutput(
    val schemaVersion: Int,
    val primitives: ExpectedPrimitives,
)

private data class ExpectedPrimitives(
    val rotatingWord: ExpectedRotatingWord,
    val challengeAnswer: ExpectedChallengeAnswer,
    val staticOverride: ExpectedStaticOverride,
)

private data class ExpectedRotatingWord(
    val enabled: Boolean,
    val intervalSeconds: Int,
    val wordFormat: String = "adjective_noun_number",
)

private data class ExpectedChallengeAnswer(val enabled: Boolean)
private data class ExpectedStaticOverride(val enabled: Boolean)
