package com.spyglass.connect.server

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.*
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ECDH key exchange + AES-256-GCM encryption for Spyglass Connect.
 *
 * Flow:
 * 1. Generate ECDH key pair (secp256r1)
 * 2. Exchange public keys during pairing
 * 3. Derive shared AES-256 key via ECDH + HKDF-SHA256
 * 4. Encrypt/decrypt all messages with AES-256-GCM (12-byte random IV)
 */
class EncryptionManager {

    companion object {
        private const val CURVE = "secp256r1"
        private const val AES_KEY_BITS = 256
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private val INFO = "spyglass-connect-v1".toByteArray()

        init {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
    }

    private val keyPair: KeyPair
    private var sharedKey: SecretKeySpec? = null
    private val secureRandom = SecureRandom()

    init {
        val spec = ECNamedCurveTable.getParameterSpec(CURVE)
        val keyGen = KeyPairGenerator.getInstance("EC", "BC")
        keyGen.initialize(spec, secureRandom)
        keyPair = keyGen.generateKeyPair()
    }

    /** Get our public key as Base64 for transmission. */
    fun getPublicKeyBase64(): String =
        Base64.getEncoder().encodeToString(keyPair.public.encoded)

    /** Generate a random nonce for the QR code. */
    fun generateNonce(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * Derive shared AES-256 key from the peer's public key.
     * Call this after receiving the phone's public key during pairing.
     */
    fun deriveSharedKey(peerPublicKeyBase64: String) {
        val peerKeyBytes = Base64.getDecoder().decode(peerPublicKeyBase64)
        val keyFactory = KeyFactory.getInstance("EC", "BC")
        val peerPublicKey = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(peerKeyBytes))

        val keyAgreement = KeyAgreement.getInstance("ECDH", "BC")
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(peerPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // HKDF-SHA256: extract + expand
        val prk = hkdfExtract(ByteArray(32), sharedSecret)
        val okm = hkdfExpand(prk, INFO, AES_KEY_BITS / 8)
        sharedKey = SecretKeySpec(okm, "AES")
    }

    /** Check if encryption is established. */
    val isReady: Boolean get() = sharedKey != null

    /** Encrypt a plaintext message. Returns Base64(IV + ciphertext + tag). */
    fun encrypt(plaintext: String): String {
        val key = sharedKey ?: throw IllegalStateException("Shared key not derived")
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Prepend IV to ciphertext
        val result = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(ciphertext, 0, result, iv.size, ciphertext.size)

        return Base64.getEncoder().encodeToString(result)
    }

    /** Decrypt a Base64(IV + ciphertext + tag) message. */
    fun decrypt(encryptedBase64: String): String {
        val key = sharedKey ?: throw IllegalStateException("Shared key not derived")
        val data = Base64.getDecoder().decode(encryptedBase64)

        val iv = data.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = data.copyOfRange(GCM_IV_BYTES, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // ── HKDF-SHA256 implementation ──────────────────────────────────────────

    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))

        val result = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var i: Byte = 1

        while (offset < length) {
            mac.update(t)
            mac.update(info)
            mac.update(i)
            t = mac.doFinal()
            val toCopy = minOf(t.size, length - offset)
            System.arraycopy(t, 0, result, offset, toCopy)
            offset += toCopy
            i++
        }

        return result
    }
}
