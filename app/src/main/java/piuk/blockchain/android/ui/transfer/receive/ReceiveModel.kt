package piuk.blockchain.android.ui.transfer.receive

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.filterByActionAndState
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.walletmode.WalletModeService
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import timber.log.Timber

class ReceiveModel(
    initialState: ReceiveState,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val walletModeService: WalletModeService,
    private val coincore: Coincore
) : MviModel<ReceiveState, ReceiveIntent>(initialState, uiScheduler, environmentConfig, remoteLogger) {

    override fun performAction(previousState: ReceiveState, intent: ReceiveIntent): Disposable? {
        return when (intent) {
            is ReceiveIntent.GetAvailableAssets -> getAvailableAccounts(intent.startForTicker)
            is ReceiveIntent.GetStartingAccountForAsset -> {
                process(
                    ReceiveIntent.UpdateReceiveForAsset(
                        intent.accounts.first {
                            it.currency.networkTicker == intent.cryptoTicker &&
                                (it is CustodialTradingAccount || it is NonCustodialAccount)
                        } as CryptoAccount
                    )
                )
                null
            }
            is ReceiveIntent.UpdateAssets,
            is ReceiveIntent.UpdateAccounts,
            is ReceiveIntent.FilterAssets,
            ReceiveIntent.ResetReceiveForAccount,
            is ReceiveIntent.UpdateReceiveForAsset -> null
        }
    }

    private fun getAvailableAccounts(startForTicker: String?): Disposable =
        walletModeService.walletModeSingle.flatMap { coincore.allWalletsInMode(it) }
            .flatMap { accountGroup ->
                accountGroup.accounts.filterByActionAndState(
                    AssetAction.Receive,
                    listOf(
                        ActionState.Available,
                        ActionState.LockedForTier
                    )
                )
                    .zipWith(coincore.activeWalletsInMode())
            }.map { (accountsList, active) ->
                accountsList.sortedWith(
                    compareBy<SingleAccount> {
                        it.currency.networkTicker !in
                            active.accounts.map { acc -> acc.currency.networkTicker }
                    }.thenByDescending { it.currency.index }
                        .thenBy { it.currency.displayTicker }
                        .thenBy { !it.isDefault }
                        .thenBy { it.label }
                )
            }.subscribeBy(
                onSuccess = { accounts ->
                    process(ReceiveIntent.UpdateAccounts(accounts))

                    startForTicker?.let { ticker ->
                        process(ReceiveIntent.GetStartingAccountForAsset(ticker, accounts))
                    }
                },
                onError = {
                    Timber.e(it)
                }
            )
}
