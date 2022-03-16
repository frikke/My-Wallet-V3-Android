package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy

class CoinViewModel(
    initialState: CoinViewState,
    mainScheduler: Scheduler,
    private val interactor: CoinViewInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<CoinViewState, CoinViewIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(previousState: CoinViewState, intent: CoinViewIntent): Disposable? =
        when (intent) {
            is CoinViewIntent.LoadAsset -> {
                interactor.loadAssetDetails(intent.assetTicker).let { (asset, fiatCurrency) ->
                    asset?.let {
                        process(CoinViewIntent.AssetLoaded(it, fiatCurrency))
                        process(CoinViewIntent.LoadAccounts(it))
                        process(CoinViewIntent.LoadRecurringBuys(it.assetInfo))
                    } ?: process(CoinViewIntent.UpdateErrorState(CoinViewError.UnknownAsset))
                }
                null
            }
            is CoinViewIntent.LoadAccounts -> loadAccounts(intent)
            is CoinViewIntent.UpdateAccountDetails -> {
                previousState.selectedFiat?.let {
                    process(CoinViewIntent.LoadAssetChart(intent.asset, intent.assetInformation.prices, it))
                }
                null
            }
            is CoinViewIntent.LoadAssetChart -> loadChart(intent)
            is CoinViewIntent.LoadNewChartPeriod ->
                previousState.asset?.let {
                    loadNewTimePeriod(it, intent, previousState)
                }
            is CoinViewIntent.LoadRecurringBuys -> loadRecurringBuys(intent)
            is CoinViewIntent.LoadQuickActions -> loadQuickActions(intent)
            CoinViewIntent.ResetErrorState,
            CoinViewIntent.ResetViewState,
            is CoinViewIntent.UpdateErrorState,
            is CoinViewIntent.UpdateViewState,
            is CoinViewIntent.AssetLoaded -> null
        }

    private fun loadRecurringBuys(intent: CoinViewIntent.LoadRecurringBuys) =
        interactor.loadRecurringBuys(intent.asset)
            .subscribeBy(
                onSuccess = {
                    process(CoinViewIntent.UpdateViewState(CoinViewViewState.ShowRecurringBuys(it)))
                },
                onError = {
                    process(CoinViewIntent.UpdateErrorState(CoinViewError.RecurringBuysLoadError))
                }
            )

    private fun loadQuickActions(intent: CoinViewIntent.LoadQuickActions) =
        interactor.loadQuickActions(intent.asset, intent.totalCryptoBalance)
            .subscribeBy(
                onSuccess = { actions ->
                    process(
                        CoinViewIntent.UpdateViewState(
                            CoinViewViewState.QuickActionsLoaded(
                                startAction = actions.first,
                                endAction = actions.second,
                                actionableAccount = intent.actionableAccount
                            )
                        )
                    )
                },
                onError = {
                    process(CoinViewIntent.UpdateErrorState(CoinViewError.QuickActionsFailed))
                }
            )

    private fun loadNewTimePeriod(
        it: CryptoAsset,
        intent: CoinViewIntent.LoadNewChartPeriod,
        previousState: CoinViewState
    ) = interactor.loadHistoricPrices(it, intent.timePeriod)
        .subscribeBy(
            onSuccess = { historicalRates ->
                process(
                    CoinViewIntent.UpdateViewState(
                        CoinViewViewState.ShowAssetInfo(
                            entries = historicalRates.map { point ->
                                ChartEntry(
                                    point.timestamp.toFloat(),
                                    point.rate.toFloat()
                                )
                            },
                            prices = previousState.assetPrices ?: throw IllegalStateException(
                                "previous state prices cant be null"
                            ),
                            historicalRateList = historicalRates,
                            selectedFiat = previousState.selectedFiat ?: throw IllegalStateException(
                                "previous state selected fiat cant be null"
                            )
                        )
                    )
                )
            },
            onError = {
                process(CoinViewIntent.UpdateErrorState(CoinViewError.ChartLoadError))
            }
        )

    private fun loadChart(intent: CoinViewIntent.LoadAssetChart) =
        interactor.loadHistoricPrices(intent.asset, HistoricalTimeSpan.DAY)
            .subscribeBy(
                onSuccess = { list ->
                    process(
                        CoinViewIntent.UpdateViewState(
                            CoinViewViewState.ShowAssetInfo(
                                list.map { point ->
                                    ChartEntry(
                                        point.timestamp.toFloat(),
                                        point.rate.toFloat()
                                    )
                                },
                                intent.assetPrice,
                                list,
                                intent.selectedFiat
                            )
                        )
                    )
                },
                onError = {
                    process(CoinViewIntent.UpdateErrorState(CoinViewError.ChartLoadError))
                }
            )

    private fun loadAccounts(intent: CoinViewIntent.LoadAccounts) =
        interactor.loadAccountDetails(intent.asset)
            .subscribeBy(
                onSuccess = { accountInfo ->
                    process(
                        CoinViewIntent.UpdateAccountDetails(
                            viewState = when (accountInfo) {
                                is AssetInformation.AccountsInfo -> CoinViewViewState.ShowAccountInfo(
                                    accountInfo
                                )
                                is AssetInformation.NonTradeable -> CoinViewViewState.NonTradeableAccount
                            },
                            assetInformation = accountInfo,
                            asset = intent.asset
                        )
                    )

                    if (accountInfo is AssetInformation.AccountsInfo) {
                        accountInfo.accountsList.firstOrNull {
                            it.account is CustodialTradingAccount
                        }?.let {
                            process(
                                CoinViewIntent.LoadQuickActions(
                                    intent.asset.assetInfo, accountInfo.totalCryptoBalance, it.account
                                )
                            )
                        }
                    }
                },
                onError = {
                    process(CoinViewIntent.UpdateErrorState(CoinViewError.WalletLoadError))
                }
            )
}
