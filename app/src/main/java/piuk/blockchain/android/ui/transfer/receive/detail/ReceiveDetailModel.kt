package piuk.blockchain.android.ui.transfer.receive.detail

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.NullCryptoAddress
import com.blockchain.coincore.eth.MultiChainAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.commonarch.presentation.base.ActivityIndicator
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.utils.unsafeLazy
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

data class ReceiveDetailState(
    val account: CryptoAccount = NullCryptoAccount(),
    val cryptoAddress: CryptoAddress = NullCryptoAddress,
    val networkName: String? = null,
    val qrUri: String? = null
) : MviState {
    fun shouldShowXlmMemo() = cryptoAddress.memo != null

    fun shouldShowRotatingAddressInfo() = !account.hasStaticAddress
}

sealed class ReceiveDetailIntent : MviIntent<ReceiveDetailState>
internal class InitWithAccount(
    val cryptoAccount: CryptoAccount
) : ReceiveDetailIntent() {
    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState {
        return oldState.copy(
            account = cryptoAccount,
            cryptoAddress = NullCryptoAddress,
            qrUri = null
        )
    }
}

internal class SetNetworkName(
    private val networkName: String
) : ReceiveDetailIntent() {
    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState {
        return oldState.copy(
            networkName = networkName
        )
    }
}

internal class UpdateAddressAndGenerateQrCode(
    private val cryptoAddress: CryptoAddress,
    private val qrUri: String
) :
    ReceiveDetailIntent() {
    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState =
        oldState.copy(
            cryptoAddress = cryptoAddress,
            qrUri = qrUri
        )
}

internal object AddressError : ReceiveDetailIntent() {
    override fun reduce(oldState: ReceiveDetailState): ReceiveDetailState = oldState
}

class ReceiveDetailModel(
    private val interactor: ReceiveDetailInteractor,
    private val _activityIndicator: Lazy<ActivityIndicator?>,
    initialState: ReceiveDetailState,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<ReceiveDetailState, ReceiveDetailIntent>(
    initialState,
    uiScheduler,
    environmentConfig,
    remoteLogger
) {

    private val activityIndicator: ActivityIndicator? by unsafeLazy {
        _activityIndicator.value
    }

    override fun performAction(previousState: ReceiveDetailState, intent: ReceiveDetailIntent): Disposable? =
        when (intent) {
            is InitWithAccount -> {
                when (val account = intent.cryptoAccount) {
                    is MultiChainAccount -> { // PKW
                        process(SetNetworkName(account.l1Network.name))
                    }
                    is CustodialTradingAccount -> { // Trading Accounts
                        account.currency.l1chainTicker?.let {
                            getNetworkNameForTradingAccount(it)
                        }
                    }
                }
                handleInit(intent.cryptoAccount)
            }
            is UpdateAddressAndGenerateQrCode,
            is SetNetworkName,
            is AddressError -> null
        }

    private fun getNetworkNameForTradingAccount(currency: String): Disposable =
        interactor.getEvmNetworkForCurrency(currency)
            .trackProgress(activityIndicator)
            .subscribeBy(
                onSuccess = {
                    process(SetNetworkName(it.name))
                },
                onError = {
                    Timber.e("Unable to fetch network name for currency: $currency + error: ${it.message}")
                }
            )

    private fun handleInit(account: CryptoAccount): Disposable =
        account.receiveAddress
            .map { it as CryptoAddress }
            .trackProgress(activityIndicator)
            .subscribeBy(
                onSuccess = {
                    process(
                        UpdateAddressAndGenerateQrCode(
                            cryptoAddress = it,
                            qrUri = it.toUrl()
                        )
                    )
                },
                onError = {
                    Timber.e("Unable to fetch ${account.currency} address from account")
                    process(AddressError)
                }
            )
}
