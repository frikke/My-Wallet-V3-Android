package piuk.blockchain.android.ui.transfer.receive

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.filterByActionAndState
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.domain.usecases.GetAvailableCryptoAssetsUseCase
import piuk.blockchain.android.domain.usecases.GetReceiveAccountsForAssetUseCase
import timber.log.Timber

class ReceiveModel(
    initialState: ReceiveState,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val getAvailableCryptoAssetsUseCase: GetAvailableCryptoAssetsUseCase,
    private val walletModeService: WalletModeService,
    private val coincore: Coincore,
    private val getReceiveAccountsForAssetUseCase: GetReceiveAccountsForAssetUseCase,
) : MviModel<ReceiveState, ReceiveIntent>(initialState, uiScheduler, environmentConfig, remoteLogger) {

    override fun performAction(previousState: ReceiveState, intent: ReceiveIntent): Disposable? {
        return when (intent) {
            ReceiveIntent.GetAvailableAssets -> if (walletModeService.enabledWalletMode() == WalletMode.UNIVERSAL)
                getAvailableAssets() else getAvailableAccounts()
            is ReceiveIntent.UpdateAssets,
            is ReceiveIntent.UpdateAccounts,
            is ReceiveIntent.FilterAssets,
            -> null
        }
    }

    private fun getAvailableAssets(): Disposable =
        getAvailableCryptoAssetsUseCase(Unit)
            .subscribeBy(
                onSuccess = { assets ->
                    process(
                        ReceiveIntent.UpdateAssets(
                            assets = assets,
                            loadAccountsForAsset = ::loadAccountsForAsset
                        )
                    )
                },
                onError = { throwable ->
                    Timber.e(throwable)
                }
            )

    private fun getAvailableAccounts(): Disposable =
        coincore.allWalletsInMode(walletModeService.enabledWalletMode()).flatMap { accountGroup ->
            accountGroup.accounts.filterByActionAndState(
                AssetAction.Receive,
                listOf(ActionState.Available, ActionState.LockedForTier)
            )
        }.map { accountsList ->
            accountsList.sortedWith(
                compareBy<SingleAccount> { it.currency.displayTicker }.thenBy { it.isDefault }.thenBy { it.label }
            )
        }.subscribeBy(
            onError = {
                Timber.e(it)
            }, onSuccess = {
            process(ReceiveIntent.UpdateAccounts(it))
        }
        )

    private fun loadAccountsForAsset(assetInfo: AssetInfo): Single<List<CryptoAccount>> {
        return getReceiveAccountsForAssetUseCase(assetInfo).map {
            it.filterIsInstance<CryptoAccount>()
        }
    }
}
