package com.thc.safewords.crypto

import com.thc.safewords.data.WordLists
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * TOTP-based safeword derivation engine.
 *
 * Given a 256-bit seed and the current time, derives a deterministic
 * human-readable phrase of the form "adjective noun number".
 *
 * Algorithm:
 * 1. counter = floor(timestamp / interval)
 * 2. counter_bytes = int64 big-endian
 * 3. hash = HMAC-SHA256(key=seed, message=counter_bytes)
 * 4. Dynamic truncation to extract adjective index, noun index, and number
 */
object TOTPDerivation {

    private const val ADJECTIVE_COUNT = 197
    private const val NOUN_COUNT = 300
    private const val NUMBER_MODULUS = 100

    /**
     * Derive a safeword phrase from a seed, interval, and timestamp.
     *
     * @param seed 32-byte seed
     * @param interval rotation interval in seconds
     * @param timestamp unix epoch seconds
     * @return lowercase phrase like "breezy rocket 75"
     */
    fun deriveSafeword(seed: ByteArray, interval: Int, timestamp: Long): String {
        require(seed.size == 32) { "Seed must be 32 bytes" }
        require(interval > 0) { "Interval must be positive" }

        val counter = timestamp / interval
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(seed, "HmacSHA256"))
        val hash = mac.doFinal(counterBytes)

        val offset = hash[31].toInt() and 0x0F

        val adjIdx = ((hash[offset].toInt() and 0x7F) shl 8 or
                (hash[offset + 1].toInt() and 0xFF)) % ADJECTIVE_COUNT

        val nounIdx = ((hash[offset + 2].toInt() and 0x7F) shl 8 or
                (hash[offset + 3].toInt() and 0xFF)) % NOUN_COUNT

        val number = ((hash[offset + 4].toInt() and 0x7F) shl 8 or
                (hash[offset + 5].toInt() and 0xFF)) % NUMBER_MODULUS

        val adjective = WordLists.adjectives[adjIdx]
        val noun = WordLists.nouns[nounIdx]

        return "$adjective $noun $number"
    }

    /**
     * Get the current TOTP counter value.
     */
    fun getCurrentCounter(interval: Int): Long {
        val now = System.currentTimeMillis() / 1000
        return now / interval
    }

    /**
     * Get seconds remaining until the next rotation.
     */
    fun getTimeRemaining(interval: Int): Long {
        val now = System.currentTimeMillis() / 1000
        val nextRotation = (now / interval + 1) * interval
        return nextRotation - now
    }

    /**
     * Format remaining seconds as HH:MM:SS or MM:SS depending on magnitude.
     */
    fun formatTimeRemaining(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Parse a hex string to a byte array.
     */
    fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /**
     * Convert a byte array to a hex string.
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
