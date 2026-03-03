package com.spyglass.connect.pairing

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.spyglass.connect.model.QrPairingData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.util.Base64

/**
 * Generate QR codes containing pairing data for Spyglass Connect.
 */
object QrCodeGenerator {

    private const val QR_SIZE = 300

    /**
     * Generate a QR code image containing the pairing data as Base64-encoded JSON.
     */
    fun generate(
        ip: String,
        port: Int,
        publicKeyBase64: String,
        nonce: String,
    ): BufferedImage {
        val pairingData = QrPairingData(
            ip = ip,
            port = port,
            pubkey = publicKeyBase64,
            nonce = nonce,
        )

        val jsonString = Json.encodeToString(pairingData)
        return generateQrImage(jsonString, QR_SIZE)
    }

    /**
     * Generate a QR code BufferedImage from content string.
     */
    private fun generateQrImage(content: String, size: Int): BufferedImage {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until size) {
            for (y in 0 until size) {
                image.setRGB(x, y, if (bitMatrix.get(x, y)) 0x000000 else 0xFFFFFF)
            }
        }

        return image
    }
}
