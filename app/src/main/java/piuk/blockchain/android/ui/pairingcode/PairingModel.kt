package piuk.blockchain.android.ui.pairingcode

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.PairingEvent
import com.blockchain.notifications.analytics.PairingMethod
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import okhttp3.ResponseBody
import piuk.blockchain.android.scan.QrCodeDataManager
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class PairingModel(
    initialState: PairingState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val qrCodeDataManager: QrCodeDataManager,
    private val analytics: Analytics,
    private val payloadDataManager: PayloadDataManager,
    private val authDataManager: AuthDataManager
) : MviModel<PairingState, PairingIntents>(initialState, mainScheduler, environmentConfig, crashLogger) {

    override fun performAction(previousState: PairingState, intent: PairingIntents): Disposable? {
        return when (intent) {
            is PairingIntents.LoadQrImage -> loadQrCode()
            is PairingIntents.ShowQrImage -> showQrCode(previousState.imageStatus)
            is PairingIntents.ShowQrError,
            is PairingIntents.CompleteQrImageLoading,
            is PairingIntents.HideQrImage -> null
        }
    }

    private fun showQrCode(qrCodeImageStatus: QrCodeImageStatus): Disposable? {
        if (qrCodeImageStatus !is QrCodeImageStatus.Ready &&
            qrCodeImageStatus !is QrCodeImageStatus.Hidden
        ) {
            process(PairingIntents.LoadQrImage)
        }
        return null
    }

    private fun loadQrCode(): Disposable =
        pairingEncryptionPasswordObservable
            .flatMap { encryptionPassword -> generatePairingCodeObservable(encryptionPassword.string()) }
            .subscribe(
                { bitmap ->
                    process(PairingIntents.CompleteQrImageLoading(bitmap))
                    analytics.logEvent(PairingEvent(PairingMethod.REVERSE))
                },
                { process(PairingIntents.ShowQrError) }
            )

    private val pairingEncryptionPasswordObservable: Single<ResponseBody>
        get() = payloadDataManager.wallet?.let { wallet ->
            Single.fromObservable(authDataManager.getPairingEncryptionPassword(wallet.guid))
        } ?: Single.error(IllegalStateException("Wallet cannot be null"))

    private fun generatePairingCodeObservable(encryptionPhrase: String): Single<String> {
        check(payloadDataManager.tempPassword != null)
        return payloadDataManager.wallet?.let { wallet ->
            qrCodeDataManager.generatePairingCode(
                wallet.guid,
                payloadDataManager.tempPassword
                    ?: throw java.lang.IllegalStateException("TempPassword is missing"),
                wallet.sharedKey,
                encryptionPhrase
            )
        } ?: Single.error(IllegalStateException("Wallet cannot be null"))
    }
}
