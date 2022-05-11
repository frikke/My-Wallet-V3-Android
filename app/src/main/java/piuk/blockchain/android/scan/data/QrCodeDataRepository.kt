package piuk.blockchain.android.scan.data

import info.blockchain.wallet.crypto.AESUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.nio.charset.StandardCharsets
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.android.scan.domain.QrCodeDataService

class QrCodeDataRepository : QrCodeDataService {

    override fun generatePairingCode(
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
