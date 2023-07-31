package com.blockchain.core.utils

import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.DecryptionException
import java.io.UnsupportedEncodingException
import java.lang.Exception
import org.spongycastle.crypto.InvalidCipherTextException

object AESUtilWrapper {
    @Throws(
        UnsupportedEncodingException::class,
        InvalidCipherTextException::class,
        DecryptionException::class
    )
    fun decrypt(ciphertext: String, password: String, iterations: Int): String {
        return AESUtil.decrypt(ciphertext, password, iterations)
    }

    @Throws(Exception::class)
    fun encrypt(plaintext: String, password: String, iterations: Int): String {
        return AESUtil.encrypt(plaintext, password, iterations)
    }
}
