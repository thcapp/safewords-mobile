package com.thc.safewords.crypto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class Bip39Test {

    private val bip39: Bip39 by lazy {
        val text = readResource("/bip39-english.txt")
        val words = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        Bip39(words)
    }

    private val vectors: VectorFile by lazy {
        val text = readResource("/recovery-vectors.json")
        Gson().fromJson(text, object : TypeToken<VectorFile>() {}.type)
    }

    private fun readResource(path: String): String {
        val stream = javaClass.getResourceAsStream(path)
            ?: throw IllegalStateException("Test resource $path not on classpath. Did the gradle copySharedToTestResources task run?")
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun encode_matches_every_vector() {
        for (v in vectors.valid) {
            val seed = hexToBytes(v.seedHex)
            val phrase = bip39.encode(seed)
            assertEquals("encode(${v.id})", v.mnemonic, phrase)
        }
    }

    @Test
    fun decode_matches_every_vector() {
        for (v in vectors.valid) {
            val decoded = bip39.decode(v.mnemonic)
            assertArrayEquals("decode(${v.id})", hexToBytes(v.seedHex), decoded)
        }
    }

    @Test
    fun decode_normalizes_input_for_each_vector_with_normalizedInput() {
        for (v in vectors.valid) {
            val variant = v.normalizedInput ?: continue
            val decoded = bip39.decode(variant)
            assertArrayEquals(
                "decode(normalizedInput of ${v.id})",
                hexToBytes(v.seedHex),
                decoded
            )
        }
    }

    @Test
    fun decode_rejects_every_invalid_vector_with_documented_error() {
        for (v in vectors.invalid) {
            try {
                bip39.decode(v.input)
                fail("decode(${v.id}) should have thrown ${v.expectedError}")
            } catch (e: Bip39.Error) {
                assertEquals("error code for ${v.id}", v.expectedError, e.code)
                assertEquals("error message for ${v.id}", v.expectedMessage, e.userMessage)
                if (v.expectedWordIndex != null && e is Bip39.Error.UnknownWord) {
                    assertEquals("word index for ${v.id}", v.expectedWordIndex, e.wordIndex)
                }
                if (v.expectedUnknownWord != null && e is Bip39.Error.UnknownWord) {
                    assertEquals("unknown word for ${v.id}", v.expectedUnknownWord, e.word)
                }
            }
        }
    }

    @Test
    fun encode_then_decode_round_trips_for_random_seeds() {
        val rng = java.security.SecureRandom()
        repeat(10) {
            val seed = ByteArray(32).also { rng.nextBytes(it) }
            val phrase = bip39.encode(seed)
            val recovered = bip39.decode(phrase)
            assertArrayEquals(seed, recovered)
        }
    }

    // ─── helpers ────────────────────────────────────────────────────

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) or Character.digit(hex[i * 2 + 1], 16)).toByte()
        }

    // JSON shape mirrors /shared/recovery-vectors.json
    data class VectorFile(
        val version: Int = 0,
        val scheme: String = "",
        val wordCount: Int = 0,
        val seedBytes: Int = 0,
        val normalization: String = "",
        val valid: List<Valid> = emptyList(),
        val invalid: List<Invalid> = emptyList()
    )

    data class Valid(
        val id: String = "",
        val seedHex: String = "",
        val mnemonic: String = "",
        val normalizedInput: String? = null
    )

    data class Invalid(
        val id: String = "",
        val input: String = "",
        val expectedError: String = "",
        val expectedMessage: String = "",
        val expectedWordIndex: Int? = null,
        val expectedUnknownWord: String? = null
    )
}
