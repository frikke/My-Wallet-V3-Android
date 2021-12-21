package piuk.blockchain.android.ui.transfer.receive.detail

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.NullCryptoAddress
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

internal data class ReceiveDetailState(
    val account: CryptoAccount = NullCryptoAccount(),
    val cryptoAddress: CryptoAddress = NullCryptoAddress,
    val qrUri: String? = null,
    val displayMode: ReceiveScreenDisplayMode = ReceiveScreenDisplayMode.RECEIVE
) : MviState {
    fun shouldShowXlmMemo() = cryptoAddress.memo != null

    fun shouldShowRotatingAddressInfo() = !account.hasStaticAddress
}

internal sealed class ReceiveDetailIntent : MviIntent<ReceiveDetailState>
internal class InitWithAccount(
    val cryptoAccount: CryptoAccount
) : ReceiveDetailIntent() {
    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState {
        return oldState.copy(
            account = cryptoAccount,
            cryptoAddress = NullCryptoAddress,
            qrUri = null,
            displayMode = ReceiveScreenDisplayMode.RECEIVE
        )
    }
}

internal class UpdateAddressAndGenerateQrCode(val cryptoAddress: CryptoAddress) : ReceiveDetailIntent() {
    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState =
        oldState.copy(
            cryptoAddress = cryptoAddress,
            displayMode = ReceiveScreenDisplayMode.RECEIVE
        )
}

internal class UpdateQrCodeUri(private val qrUri: String) : ReceiveDetailIntent() {
    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState =
        oldState.copy(
            qrUri = qrUri,
            displayMode = ReceiveScreenDisplayMode.RECEIVE
        )
}

internal object ShowShare : ReceiveDetailIntent() {
    override fun isValidFor(oldState: ReceiveDetailState): Boolean = oldState.qrUri != null

    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState = oldState.copy(
        displayMode = ReceiveScreenDisplayMode.SHARE
    )
}

internal object AddressError : ReceiveDetailIntent() {
    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState = oldState
}

internal object ClearShareList : ReceiveDetailIntent() {
    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState =
        oldState.copy(displayMode = ReceiveScreenDisplayMode.RECEIVE)
}

internal class ReceiveDetailModel(
    initialState: ReceiveDetailState,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<ReceiveDetailState, ReceiveDetailIntent>(
    initialState,
    uiScheduler,
    environmentConfig,
    crashLogger
) {

    override fun performAction(previousState: ReceiveDetailState, intent: ReceiveDetailIntent): Disposable? =
        when (intent) {
            is InitWithAccount -> handleInit(intent.cryptoAccount)
            is UpdateAddressAndGenerateQrCode -> {
                process(UpdateQrCodeUri(intent.cryptoAddress.toUrl()))
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
                    Timber.e("Unable to fetch ${account.currency} address from account")
                    process(AddressError)
                }
            )
}

enum class ReceiveScreenDisplayMode {
    RECEIVE, SHARE
}
