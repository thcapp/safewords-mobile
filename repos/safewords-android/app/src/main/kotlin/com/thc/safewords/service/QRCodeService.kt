package com.thc.safewords.service

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.thc.safewords.model.Group
import com.thc.safewords.model.RotationInterval

/**
 * Service for generating and parsing QR codes for group sharing.
 *
 * QR payload format (JSON):
 * {
 *   "v": 1,
 *   "name": "Johnson Family",
 *   "seed": "<base64url-encoded 256-bit seed>",
 *   "interval": "daily"
 * }
 */
object QRCodeService {

    private val gson = Gson()

    data class QRPayload(
        val v: Int = 1,
        val name: String,
        val seed: String,
        val interval: String
    )

    /**
     * Generate a QR code bitmap for sharing a group's seed.
     *
     * @param group the group to share
     * @param size QR code size in pixels
     * @return Bitmap of the QR code
     */
    fun generateQRBitmap(group: Group, size: Int = 512): Bitmap? {
        val seedHex = SecureStorageService.getSeed(group.id) ?: return null
        val seedBytes = hexToBytes(seedHex)
        val seedBase64 = Base64.encodeToString(
            seedBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        val payload = QRPayload(
            name = group.name,
            seed = seedBase64,
            interval = group.interval.key
        )

        val json = gson.toJson(payload)

        return try {
            val writer = QRCodeWriter()
            val hints = mapOf(
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix: BitMatrix = writer.encode(json, BarcodeFormat.QR_CODE, size, size, hints)
            toBitmap(bitMatrix)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse a QR code string payload into group creation data.
     *
     * @param rawPayload the raw string from the QR code
     * @return parsed payload, or null if invalid
     */
    fun parseQRPayload(rawPayload: String): ParsedGroup? {
        return try {
            val payload = gson.fromJson(rawPayload, QRPayload::class.java)
            if (payload.v != 1) return null

            val seedBytes = Base64.decode(
                payload.seed,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            if (seedBytes.size != 32) return null

            val seedHex = bytesToHex(seedBytes)
            val interval = RotationInterval.fromKey(payload.interval)

            ParsedGroup(
                name = payload.name,
                seedHex = seedHex,
                interval = interval
            )
        } catch (e: Exception) {
            null
        }
    }

    data class ParsedGroup(
        val name: String,
        val seedHex: String,
        val interval: RotationInterval
    )

    private fun toBitmap(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix.get(x, y)) {
                    0xFF000000.toInt()
                } else {
                    0xFFFFFFFF.toInt()
                }
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
