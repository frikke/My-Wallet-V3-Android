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
import com.blockchain.core.price.ExchangeRatesDataManager
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
    private val exchangeRatesDataManager: ExchangeRatesDataManager
) : MviModel<ReceiveState, ReceiveIntent>(initialState, uiScheduler, environmentConfig, remoteLogger) {

    override fun performAction(previousState: ReceiveState, intent: ReceiveIntent): Disposable? {
        return when (intent) {
            is ReceiveIntent.GetAvailableAssets -> if (walletModeService.enabledWalletMode() == WalletMode.UNIVERSAL) {
                // Receive to an account not supported on non-SuperApp MVP
                getAvailableAssets()
            } else {
                getAvailableAccounts(intent.startForTicker)
            }
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

    private fun getAvailableAssets(): Disposable =
        getAvailableCryptoAssetsUseCase(Unit).flatMap { assets ->
            Single.concat(
                assets.map { fetchAssetPrice(it) }
            ).toList()
        }
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

    private fun getAvailableAccounts(startForTicker: String?): Disposable =
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

    private fun loadAccountsForAsset(assetInfo: AssetInfo): Single<List<CryptoAccount>> {
        return getReceiveAccountsForAssetUseCase(assetInfo).map {
            it.filterIsInstance<CryptoAccount>()
        }
    }

    private fun fetchAssetPrice(assetInfo: AssetInfo): Single<ReceiveItem> {
        return exchangeRatesDataManager.getPricesWith24hDeltaLegacy(assetInfo).firstOrError()
            .map {
                ReceiveItem(
                    assetInfo = assetInfo,
                    priceWithDelta = it
                )
            }
            .onErrorReturn {
                Timber.e(it)
                ReceiveItem(
                    assetInfo = assetInfo,
                    priceWithDelta = null
                )
            }
    }
}
