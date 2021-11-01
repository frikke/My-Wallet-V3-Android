package piuk.blockchain.android.scan

import info.blockchain.wallet.crypto.AESUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.spongycastle.util.encoders.Hex
import java.nio.charset.StandardCharsets

class QrCodeDataManager {
    /**
     * Generates a pairing QR code in Bitmap format from a given password, sharedkey and encryption
     * phrase to specified dimensions, wrapped in a Single. Will throw an error if the Bitmap
     * is null.
     *
     * @param password Wallet's plain text password
     * @param sharedKey Wallet's plain text sharedkey
     * @param encryptionPhrase The pairing encryption password
     */
    fun generatePairingCode(
        guid: String,
        password: String,
        sharedKey: String,
        encryptionPhrase: String
    ): Single<String> {
        return generatePairingCodeUri(guid, password, sharedKey, encryptionPhrase)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun generatePairingCodeUri(
        guid: String,
        password: String,
        sharedKey: String,
        encryptionPhrase: String
    ): Single<String> {
        return Single.fromCallable {
            val pwHex = Hex.toHexString(password.toByteArray(StandardCharsets.UTF_8))
            val encrypted = AESUtil.encrypt("$sharedKey|$pwHex", encryptionPhrase, PAIRING_CODE_PBKDF2_ITERATIONS)
            "1|$guid|$encrypted"
        }
    }

    companion object {
        private const val PAIRING_CODE_PBKDF2_ITERATIONS = 10
    }
}