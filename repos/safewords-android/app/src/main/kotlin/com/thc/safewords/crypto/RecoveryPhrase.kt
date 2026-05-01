package com.thc.safewords.crypto

import com.thc.safewords.SafewordsApp
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/**
 * BIP39 English 24-word recovery phrase encoding for 256-bit (32-byte) Safewords
 * group seeds. Implements the contract at /shared/recovery-schema.md.
 *
 * Entropy-only: this does NOT derive a 64-byte BIP39 seed via PBKDF2/passphrase.
 * Decoding a valid phrase returns the original 32-byte group seed directly.
 */
class Bip39(private val wordlist: List<String>) {

    init {
        if (wordlist.size != EXPECTED_VOCAB_SIZE) {
            throw Error.InvalidWordlistCount(wordlist.size)
        }
    }

    private val wordIndex: Map<String, Int> = wordlist.withIndex().associate { (i, w) -> w to i }

    sealed class Error(val code: String, val userMessage: String) : RuntimeException(userMessage) {
        data object EmptyInput : Error(
            code = "EMPTY_INPUT",
            userMessage = "Enter your 24-word recovery phrase."
        )
        data object WrongWordCount : Error(
            code = "WRONG_WORD_COUNT",
            userMessage = "Recovery phrase must be exactly 24 words."
        )
        class UnknownWord(val wordIndex: Int, val word: String) : Error(
            code = "UNKNOWN_WORD",
            userMessage = "Word $wordIndex is not in the recovery word list: \"$word\"."
        )
        data object BadChecksum : Error(
            code = "BAD_CHECKSUM",
            userMessage = "Recovery phrase checksum is invalid. Check the words and order."
        )
        data object MissingWordlist : Error(
            code = "MISSING_WORDLIST",
            userMessage = "Recovery word list is unavailable. Use the raw seed backup or reinstall the app."
        )
        class InvalidWordlistCount(val count: Int) : Error(
            code = "INVALID_WORDLIST",
            userMessage = "Recovery word list is invalid. Expected 2048 words, found $count."
        )
    }

    /**
     * Encode a 32-byte seed as a 24-word BIP39 English phrase.
     */
    fun encode(seed: ByteArray): String {
        require(seed.size == EXPECTED_ENTROPY_BYTES) {
            "Seed must be $EXPECTED_ENTROPY_BYTES bytes (got ${seed.size})"
        }
        val checksumByte = sha256(seed)[0]
        val indices = IntArray(EXPECTED_WORD_COUNT)
        var bitBuffer = 0L
        var bitsInBuffer = 0
        var indexCursor = 0
        // Push entropy bytes
        for (b in seed) {
            bitBuffer = (bitBuffer shl 8) or (b.toLong() and 0xFF)
            bitsInBuffer += 8
            while (bitsInBuffer >= BITS_PER_WORD) {
                val shift = bitsInBuffer - BITS_PER_WORD
                indices[indexCursor++] = ((bitBuffer ushr shift) and 0x7FF).toInt()
                bitsInBuffer -= BITS_PER_WORD
                bitBuffer = bitBuffer and ((1L shl bitsInBuffer) - 1)
            }
        }
        // Push 8 checksum bits
        bitBuffer = (bitBuffer shl CHECKSUM_BITS) or (checksumByte.toLong() and 0xFF)
        bitsInBuffer += CHECKSUM_BITS
        while (bitsInBuffer >= BITS_PER_WORD) {
            val shift = bitsInBuffer - BITS_PER_WORD
            indices[indexCursor++] = ((bitBuffer ushr shift) and 0x7FF).toInt()
            bitsInBuffer -= BITS_PER_WORD
            bitBuffer = bitBuffer and ((1L shl bitsInBuffer) - 1)
        }
        check(indexCursor == EXPECTED_WORD_COUNT) {
            "Bit-packing produced $indexCursor words, expected $EXPECTED_WORD_COUNT"
        }
        return indices.joinToString(" ") { wordlist[it] }
    }

    /**
     * Decode a 24-word phrase back to its 32-byte seed.
     * Throws [Error] subclasses on invalid input.
     */
    fun decode(input: String): ByteArray {
        val tokens = normalize(input)
        if (tokens.isEmpty()) throw Error.EmptyInput
        if (tokens.size != EXPECTED_WORD_COUNT) throw Error.WrongWordCount

        val indices = IntArray(EXPECTED_WORD_COUNT)
        for ((i, token) in tokens.withIndex()) {
            val idx = wordIndex[token] ?: throw Error.UnknownWord(wordIndex = i + 1, word = token)
            indices[i] = idx
        }

        // Rebuild 264 bits from 24 11-bit indices
        val seed = ByteArray(EXPECTED_ENTROPY_BYTES)
        var bitBuffer = 0L
        var bitsInBuffer = 0
        var byteCursor = 0
        for (idx in indices) {
            bitBuffer = (bitBuffer shl BITS_PER_WORD) or (idx.toLong() and 0x7FF)
            bitsInBuffer += BITS_PER_WORD
            while (bitsInBuffer >= 8 && byteCursor < EXPECTED_ENTROPY_BYTES) {
                val shift = bitsInBuffer - 8
                seed[byteCursor++] = ((bitBuffer ushr shift) and 0xFF).toByte()
                bitsInBuffer -= 8
                bitBuffer = bitBuffer and ((1L shl bitsInBuffer) - 1)
            }
        }
        // Remaining bitsInBuffer bits are the checksum (should be 8)
        check(bitsInBuffer == CHECKSUM_BITS) {
            "Bit-unpacking left $bitsInBuffer trailing bits, expected $CHECKSUM_BITS"
        }
        val providedChecksum = (bitBuffer and 0xFF).toInt()
        val expectedChecksum = sha256(seed)[0].toInt() and 0xFF
        if (providedChecksum != expectedChecksum) throw Error.BadChecksum

        return seed
    }

    /**
     * Apply schema-mandated normalization to user input:
     * NFKD → trim → Locale.ROOT lowercase → split on whitespace.
     */
    fun normalize(input: String): List<String> {
        val nfkd = Normalizer.normalize(input, Normalizer.Form.NFKD)
        val lower = nfkd.trim().lowercase(Locale.ROOT)
        if (lower.isEmpty()) return emptyList()
        return lower.split(Regex("\\s+")).filter { it.isNotEmpty() }
    }

    private fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)

    companion object {
        const val EXPECTED_WORD_COUNT = 24
        const val EXPECTED_ENTROPY_BYTES = 32
        const val EXPECTED_VOCAB_SIZE = 2048
        private const val CHECKSUM_BITS = 8
        private const val BITS_PER_WORD = 11
    }
}

/**
 * App-runtime singleton that loads the BIP39 English wordlist from app assets.
 * For unit tests, construct [Bip39] directly with a wordlist read from disk.
 */
object RecoveryPhrase {
    /**
     * Lazy-loads the wordlist on first use. If the asset is missing or
     * malformed, surfaces the failure as a Bip39.Error so callers can fall
     * back to hex seed display rather than crashing the app.
     */
    private val instanceResult: Result<Bip39> by lazy {
        runCatching {
            val stream = try {
                SafewordsApp.instance.assets.open("wordlists/bip39-english.txt")
            } catch (_: Throwable) {
                throw Bip39.Error.MissingWordlist
            }
            val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val words = raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            Bip39(words)
        }
    }

    private fun instance(): Bip39 = instanceResult.getOrThrow()

    fun encode(seed: ByteArray): String = instance().encode(seed)
    fun decode(input: String): ByteArray = instance().decode(input)
    fun normalize(input: String): List<String> = instance().normalize(input)
}
