package com.thc.safewords.crypto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

class PrimitivesTest {

    private val vectors: PrimitiveVectorsFile by lazy {
        val text = readResource("/primitive-vectors.json")
        Gson().fromJson(text, object : TypeToken<PrimitiveVectorsFile>() {}.type)
    }

    private fun seedHex(name: String): String = when (name) {
        "seedA" -> vectors.fixtures.seedA
        "seedB" -> vectors.fixtures.seedB
        else -> error("Unknown fixture seed: $name")
    }

    private fun bytes(name: String): ByteArray = TOTPDerivation.hexToBytes(seedHex(name))

    @Test
    fun static_override_matches_every_vector() {
        for (v in vectors.staticOverrideVectors) {
            val phrase = Primitives.staticOverride(bytes(v.seed))
            assertEquals("staticOverride(${v.seed})", v.phrase, phrase)
        }
    }

    @Test
    fun numeric_matches_every_vector() {
        for (v in vectors.numericVectors) {
            val code = Primitives.numeric(bytes(v.seed), v.interval, v.timestamp)
            assertEquals("numeric(${v.seed}, ${v.interval}, ${v.timestamp})", v.code, code)
        }
    }

    @Test
    fun challenge_answer_matches_every_vector() {
        for (v in vectors.challengeAnswerVectors) {
            val row = Primitives.challengeAnswerRow(bytes(v.seed), v.tableVersion, v.rowIndex)
            assertEquals("ca.ask(${v.seed}, v${v.tableVersion}, row=${v.rowIndex})", v.ask.phrase, row.ask)
            assertEquals("ca.expect(${v.seed}, v${v.tableVersion}, row=${v.rowIndex})", v.expect.phrase, row.expect)
        }
    }

    @Test
    fun challenge_answer_table_returns_requested_row_count() {
        val seed = bytes("seedA")
        val table = Primitives.challengeAnswerTable(seed, 1, 24)
        assertEquals(24, table.size)
        assertEquals(0, table.first().rowIndex)
        assertEquals(23, table.last().rowIndex)
    }

    @Test
    fun static_override_is_deterministic_for_same_seed() {
        val seed = bytes("seedA")
        val a = Primitives.staticOverride(seed)
        val b = Primitives.staticOverride(seed)
        assertEquals(a, b)
    }

    private fun readResource(path: String): String {
        val stream = javaClass.getResourceAsStream(path)
            ?: throw IllegalStateException("Test resource $path not on classpath. Did the gradle copySharedToTestResources task run?")
        return stream.bufferedReader().use { it.readText() }
    }
}

private data class PrimitiveVectorsFile(
    val version: Int,
    val fixtures: PrimitiveFixtures,
    val staticOverrideVectors: List<StaticOverrideVector>,
    val numericVectors: List<NumericVector>,
    val challengeAnswerVectors: List<ChallengeAnswerVector>,
)

private data class PrimitiveFixtures(val seedA: String, val seedB: String)

private data class StaticOverrideVector(
    val seed: String,
    val hashHex: String,
    val offset: Int,
    val adjIndex: Int,
    val nounIndex: Int,
    val number: Int,
    val phrase: String,
)

private data class NumericVector(
    val seed: String,
    val interval: Int,
    val timestamp: Long,
    val note: String?,
    val counter: Long,
    val hashHex: String,
    val offset: Int,
    val code: String,
)

private data class ChallengeAnswerVector(
    val seed: String,
    val tableVersion: Int,
    val rowIndex: Int,
    val note: String?,
    val ask: WordPart,
    val expect: WordPart,
)

private data class WordPart(
    val hashHex: String,
    val offset: Int,
    val adjIndex: Int,
    val nounIndex: Int,
    val number: Int,
    val phrase: String,
)
