package info.blockchain.wallet.ethereum.util

import org.spongycastle.util.encoders.Hex

object EthUtils {

    fun convertHexToBigInteger(hex: String) = hex.removePrefix(PREFIX).toBigInteger(RADIX)

    fun decorateAndEncode(signedBytes: ByteArray) = PREFIX + String(Hex.encode(signedBytes))

    const val PREFIX = "0x"
    const val RADIX = 16
}
