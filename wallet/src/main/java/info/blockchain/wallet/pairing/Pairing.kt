package info.blockchain.wallet.pairing

import info.blockchain.wallet.crypto.AESUtil
import java.lang.Exception
import java.util.regex.Pattern

object Pairing {
    /**
     * @param rawString
     * @return Pair. Left = guid, Right = encryptedPairingCode
     * @throws Exception
     */
    fun qRComponentsFromRawString(rawString: String): Pair<String, String> {
        if (rawString == null || rawString.length == 0 || rawString[0] != '1') {
            throw Exception("QR string null or empty.")
        }
        val components = rawString.split("\\|".toRegex(), Pattern.LITERAL.coerceAtLeast(0)).toTypedArray()
        if (components.size != 3) {
            throw Exception("QR string does not have 3 components.")
        }
        if (components[1].length != 36) {
            throw Exception("GUID should be 36 characters in length.")
        }
        return Pair(components[1], components[2])
    }

    fun sharedKeyAndPassword(
        encryptedPairingCode: String,
        encryptionPassword: String
    ): Array<String> {
        val decryptedPairingCode = AESUtil.decrypt(
            encryptedPairingCode,
            encryptionPassword,
            AESUtil.QR_CODE_PBKDF_2ITERATIONS
        )
            ?: throw Exception("Pairing code decryption failed.")
        val sharedKeyAndPassword =
            decryptedPairingCode.split("\\|".toRegex(), Pattern.LITERAL.coerceAtLeast(0)).toTypedArray()
        if (sharedKeyAndPassword.size < 2) {
            throw Exception("Invalid decrypted pairing code.")
        }
        val sharedKey = sharedKeyAndPassword[0]
        if (sharedKey.length != 36) {
            throw Exception("Invalid shared key.")
        }
        return sharedKeyAndPassword
    }
}
