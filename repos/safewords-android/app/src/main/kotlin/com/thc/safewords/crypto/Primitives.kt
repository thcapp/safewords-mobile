package com.thc.safewords.crypto

import com.thc.safewords.data.WordLists
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Primitives {

    private const val ADJECTIVE_COUNT = 197
    private const val NOUN_COUNT = 300
    private const val NUMBER_MODULUS = 100

    private const val OVERRIDE_LABEL = "safewords/static-override/v1"

    fun staticOverride(seed: ByteArray): String {
        require(seed.size == 32) { "Seed must be 32 bytes" }
        val hash = hmacSha256(seed, OVERRIDE_LABEL.toByteArray(Charsets.UTF_8))
        return phraseFromHash(hash)
    }

    fun numeric(seed: ByteArray, intervalSeconds: Int, timestamp: Long): String {
        require(seed.size == 32) { "Seed must be 32 bytes" }
        require(intervalSeconds > 0) { "Interval must be positive" }
        val counter = timestamp / intervalSeconds
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
        val hash = hmacSha256(seed, counterBytes)
        val offset = hash[31].toInt() and 0x0F
        val codeInt = (
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
            ) % 1_000_000
        return "%06d".format(codeInt)
    }

    data class ChallengeAnswerRow(val rowIndex: Int, val ask: String, val expect: String)

    fun challengeAnswerRow(seed: ByteArray, tableVersion: Int, rowIndex: Int): ChallengeAnswerRow {
        require(seed.size == 32) { "Seed must be 32 bytes" }
        val askLabel = "safewords/challenge-answer/v$tableVersion/ask/$rowIndex"
        val expectLabel = "safewords/challenge-answer/v$tableVersion/expect/$rowIndex"
        val askHash = hmacSha256(seed, askLabel.toByteArray(Charsets.UTF_8))
        val expectHash = hmacSha256(seed, expectLabel.toByteArray(Charsets.UTF_8))
        return ChallengeAnswerRow(
            rowIndex = rowIndex,
            ask = phraseFromHash(askHash),
            expect = phraseFromHash(expectHash),
        )
    }

    fun challengeAnswerTable(seed: ByteArray, tableVersion: Int, rowCount: Int): List<ChallengeAnswerRow> =
        (0 until rowCount).map { challengeAnswerRow(seed, tableVersion, it) }

    private fun phraseFromHash(hash: ByteArray): String {
        val offset = hash[31].toInt() and 0x0F
        val adjIdx = ((hash[offset].toInt() and 0x7F) shl 8 or
            (hash[offset + 1].toInt() and 0xFF)) % ADJECTIVE_COUNT
        val nounIdx = ((hash[offset + 2].toInt() and 0x7F) shl 8 or
            (hash[offset + 3].toInt() and 0xFF)) % NOUN_COUNT
        val number = ((hash[offset + 4].toInt() and 0x7F) shl 8 or
            (hash[offset + 5].toInt() and 0xFF)) % NUMBER_MODULUS
        return "${WordLists.adjectives[adjIdx]} ${WordLists.nouns[nounIdx]} $number"
    }

    private fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(message)
    }
}
