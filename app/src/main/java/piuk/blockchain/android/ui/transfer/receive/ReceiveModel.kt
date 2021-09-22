package piuk.blockchain.android.ui.transfer.receive

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.NullCryptoAddress
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

internal data class ReceiveState(
    val account: CryptoAccount = NullCryptoAccount(),
    val address: CryptoAddress = NullCryptoAddress,
    val qrUri: String? = null,
    val displayMode: ReceiveScreenDisplayMode = ReceiveScreenDisplayMode.RECEIVE
) : MviState {
    fun shouldShowXlmMemo() = address.memo != null

    fun shouldShowRotatingAddressInfo() = !account.hasStaticAddress
}

internal sealed class ReceiveIntent : MviIntent<ReceiveState>
internal class InitWithAccount(
    val cryptoAccount: CryptoAccount
) : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState {
        return oldState.copy(
            account = cryptoAccount,
            address = NullCryptoAddress,
            qrUri = null,
            displayMode = ReceiveScreenDisplayMode.RECEIVE
        )
    }
}

internal class UpdateAddressAndGenerateQrCode(val address: CryptoAddress) : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState =
        oldState.copy(
            address = address,
            displayMode = ReceiveScreenDisplayMode.RECEIVE
        )
}

internal class UpdateQrCodeUri(private val qrUri: String) : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState =
        oldState.copy(
            qrUri = qrUri,
            displayMode = ReceiveScreenDisplayMode.RECEIVE
        )
}

internal object ShowShare : ReceiveIntent() {
    override fun isValidFor(oldState: ReceiveState): Boolean = oldState.qrUri != null

    override fun reduce(oldState: ReceiveState): ReceiveState = oldState.copy(
        displayMode = ReceiveScreenDisplayMode.SHARE
    )
}

internal object AddressError : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState = oldState
}

internal object ClearShareList : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState =
        oldState.copy(displayMode = ReceiveScreenDisplayMode.RECEIVE)
}

internal class ReceiveModel(
    initialState: ReceiveState,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<ReceiveState, ReceiveIntent>(
    initialState,
    uiScheduler,
    environmentConfig,
    crashLogger
) {

    override fun performAction(previousState: ReceiveState, intent: ReceiveIntent): Disposable? =
        when (intent) {
            is InitWithAccount -> handleInit(intent.cryptoAccount)
            is UpdateAddressAndGenerateQrCode -> {
                process(UpdateQrCodeUri(intent.address.toUrl()))
                null
            }
            is ShowShare,
            is UpdateQrCodeUri,
            is AddressError,
            is ClearShareList -> null
        }

    private fun handleInit(account: CryptoAccount): Disposable =
        account.receiveAddress
            .map { it as CryptoAddress }
            .subscribeBy(
                onSuccess = {
                    process(UpdateAddressAndGenerateQrCode(it))
                },
                onError = {
                    Timber.e("Unable to fetch ${account.asset} address from account")
                    process(AddressError)
                }
            )
}

enum class ReceiveScreenDisplayMode {
    RECEIVE, SHARE
}
