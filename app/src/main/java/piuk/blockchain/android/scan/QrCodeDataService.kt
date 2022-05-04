package piuk.blockchain.android.scan

import io.reactivex.rxjava3.core.Single

interface QrCodeDataService {
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
    ): Single<String>
}
