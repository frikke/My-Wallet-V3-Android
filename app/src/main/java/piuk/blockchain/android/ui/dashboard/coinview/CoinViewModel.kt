package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class CoinViewModel(
    initialState: CoinViewState,
    mainScheduler: Scheduler,
    private val interactor: CoinViewInteractor,
    environmentConfig: EnvironmentConfig,
    private val walletModeService: WalletModeService,
    private val remoteLogger: RemoteLogger
) : MviModel<CoinViewState, CoinViewIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {
    override fun performAction(previousState: CoinViewState, intent: CoinViewIntent): Disposable? =
        when (intent) {
            is CoinViewIntent.LoadAsset -> {
                interactor.loadAssetDetails(intent.assetTicker).subscribeBy(
                    onSuccess = { (asset, fiatCurrency) ->
                        asset?.let {
                            process(CoinViewIntent.AssetLoaded(it, fiatCurrency))
                            process(CoinViewIntent.LoadAccounts(it))
                            process(CoinViewIntent.LoadRecurringBuys(it.currency))
                            process(CoinViewIntent.LoadAssetDetails(it.currency))
                        } ?: process(CoinViewIntent.UpdateErrorState(CoinViewError.UnknownAsset))
                    },
                    onError = {
                        process(CoinViewIntent.UpdateErrorState(CoinViewError.UnknownAsset))
                    }
                )
            }
            is CoinViewIntent.LoadAccounts -> loadAccounts(intent)
            is CoinViewIntent.UpdateAccountDetails -> {
                previousState.selectedFiat?.let {
                    process(CoinViewIntent.LoadAssetChart(intent.asset, intent.assetInformation.prices, it))
                } ?: process(CoinViewIntent.UpdateErrorState(CoinViewError.MissingSelectedFiat))
                null
            }
            is CoinViewIntent.LoadAssetChart -> loadHistoricPrices(
                cryptoAsset = intent.asset,
                timeSpan = HistoricalTimeSpan.DAY,
                prices24Hr = intent.assetPrice,
                fiatCurrency = intent.selectedFiat
            )
            is CoinViewIntent.LoadNewChartPeriod -> {
                previousState.asset?.let {
                    loadNewTimePeriod(it, intent, previousState)
                } ?: process(CoinViewIntent.UpdateErrorState(CoinViewError.UnknownAsset))
                null
            }
            is CoinViewIntent.LoadRecurringBuys ->
                if
                (walletModeService.enabledWalletMode().custodialEnabled) {
                    loadRecurringBuys(intent)
                } else null
            is CoinViewIntent.LoadQuickActions -> loadQuickActions(intent)
            is CoinViewIntent.ToggleWatchlist -> toggleWatchlist(previousState)
            is CoinViewIntent.CheckScreenToOpen -> getAccountActions(intent)
            is CoinViewIntent.CheckBuyStatus -> checkUserBuyStatus()
            is CoinViewIntent.LoadAssetDetails -> loadAssetInfoDetails(intent)
            CoinViewIntent.ResetErrorState,
            CoinViewIntent.ResetViewState,
            is CoinViewIntent.UpdateWatchlistState,
            CoinViewIntent.BuyHasWarning,
            is CoinViewIntent.UpdateErrorState,
            is CoinViewIntent.UpdateViewState,
            is CoinViewIntent.AssetLoaded -> null
        }

    private fun toggleWatchlist(previousState: CoinViewState) =
        previousState.asset?.currency?.let {
            if (previousState.isAddedToWatchlist) {
                interactor.removeFromWatchlist(it)
                    .subscribeBy(
                        onComplete = {
                            process(CoinViewIntent.UpdateWatchlistState(isAddedToWatchlist = false))
                            process(
                                CoinViewIntent.UpdateViewState(
                                    CoinViewViewState.UpdatedWatchlist(addedToWatchlist = false)
                                )
                            )
                        },
                        onError = {
                            process(CoinViewIntent.UpdateErrorState(CoinViewError.WatchlistUpdateFailed))
                        }
                    )
            } else {
                interactor.addToWatchlist(it)
                    .subscribeBy(
                        onSuccess = {
                            process(CoinViewIntent.UpdateWatchlistState(isAddedToWatchlist = true))
                            process(
                                CoinViewIntent.UpdateViewState(
                                    CoinViewViewState.UpdatedWatchlist(addedToWatchlist = true)
                                )
                            )
                        },
                        onError = {
                            process(CoinViewIntent.UpdateErrorState(CoinViewError.WatchlistUpdateFailed))
                        }
                    )
            }
        }

    private fun loadAssetInfoDetails(intent: CoinViewIntent.LoadAssetDetails) =
        interactor.loadAssetInformation(intent.asset).subscribeBy(
            onSuccess = {
                process(CoinViewIntent.UpdateViewState(CoinViewViewState.ShowAssetDetails(it)))
            },
            onError = {
                process(CoinViewIntent.UpdateErrorState(CoinViewError.AssetDetailsLoadError))
            }
        )

    private fun getAccountActions(intent: CoinViewIntent.CheckScreenToOpen) =
        interactor.getAccountActions(intent.cryptoAccountSelected.account)
            .subscribeBy(
                onSuccess = { screenToNavigate ->
                    process(CoinViewIntent.UpdateViewState(screenToNavigate))
                },
                onError = {
                    Timber.e("***> Error Loading account actions: $it")
                    process(CoinViewIntent.UpdateErrorState(CoinViewError.ActionsLoadError))
                }
            )

    private fun checkUserBuyStatus() = interactor.checkIfUserCanBuy().subscribeBy(
        onSuccess = {
            if ((it as? FeatureAccess.Blocked)?.reason is BlockedReason.TooManyInFlightTransactions) {
                process(CoinViewIntent.BuyHasWarning)
            }
        },
        onError = {
            remoteLogger.logException(it, "CoinViewModel userCanBuy failed")
        }
    )

    private fun loadRecurringBuys(intent: CoinViewIntent.LoadRecurringBuys) =
        interactor.loadRecurringBuys(intent.asset)
            .subscribeBy(
                onSuccess = {
                    process(
                        CoinViewIntent.UpdateViewState(
                            CoinViewViewState.ShowRecurringBuys(recurringBuys = it.first, shouldShowUpsell = it.second)
                        )
                    )
                },
                onError = {
                    process(CoinViewIntent.UpdateErrorState(CoinViewError.RecurringBuysLoadError))
                }
            )

    private fun loadQuickActions(intent: CoinViewIntent.LoadQuickActions) =
        interactor.loadQuickActions(intent.totalCryptoBalance, intent.accountList, intent.asset)
            .subscribeBy(
                onSuccess = { actions ->
                    process(
                        CoinViewIntent.UpdateViewState(
                            CoinViewViewState.QuickActionsLoaded(
                                middleAction = actions.middleAction,
                                startAction = actions.startAction,
                                endAction = actions.endAction,
                                actionableAccount = actions.actionableAccount
                            )
                        )
                    )
                },
                onError = {
                    process(CoinViewIntent.UpdateErrorState(CoinViewError.QuickActionsFailed))
                }
            )

    private fun loadNewTimePeriod(
        cryptoAsset: CryptoAsset,
        intent: CoinViewIntent.LoadNewChartPeriod,
        previousState: CoinViewState
    ) {
        when {
            previousState.assetPrices == null -> process(
                CoinViewIntent.UpdateErrorState(CoinViewError.MissingAssetPrices)
            )
            previousState.selectedFiat == null -> process(
                CoinViewIntent.UpdateErrorState(CoinViewError.MissingSelectedFiat)
            )
            else -> {
                loadHistoricPrices(
                    cryptoAsset = cryptoAsset,
                    timeSpan = intent.timePeriod,
                    prices24Hr = previousState.assetPrices,
                    fiatCurrency = previousState.selectedFiat
                )
            }
        }
    }

    private fun loadHistoricPrices(
        cryptoAsset: CryptoAsset,
        timeSpan: HistoricalTimeSpan,
        prices24Hr: Prices24HrWithDelta,
        fiatCurrency: FiatCurrency
    ): Disposable =
        interactor.loadHistoricPrices(asset = cryptoAsset, timeSpan = timeSpan)
            .subscribeBy(
                onNext = { dataResource: DataResource<HistoricalRateList> ->
                    when (dataResource) {
                        is DataResource.Data -> {
                            if (dataResource.data.isEmpty()) {
                                process(CoinViewIntent.UpdateErrorState(CoinViewError.ChartLoadError))
                            } else {
                                process(
                                    CoinViewIntent.UpdateViewState(
                                        CoinViewViewState.ShowAssetInfo(
                                            entries = dataResource.data.map { point ->
                                                ChartEntry(
                                                    point.timestamp.toFloat(),
                                                    point.rate.toFloat()
                                                )
                                            },
                                            prices = prices24Hr,
                                            historicalRateList = dataResource.data,
                                            selectedFiat = fiatCurrency
                                        )
                                    )
                                )
                            }
                        }

                        is DataResource.Error -> {
                            process(CoinViewIntent.UpdateErrorState(CoinViewError.ChartLoadError))
                        }

                        DataResource.Loading -> {
                        }
                    }
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
                                    totalCryptoBalance = accountInfo.totalCryptoBalance,
                                    totalFiatBalance = accountInfo.totalFiatBalance,
                                    assetDetails = accountInfo.accountsList.map { assetDisplayInfo: AssetDisplayInfo ->
                                        when (assetDisplayInfo) {
                                            is AssetDisplayInfo.BrokerageDisplayInfo -> {
                                                AssetDetailsItem.BrokerageDetailsInfo(
                                                    assetFilter = assetDisplayInfo.filter,
                                                    account = assetDisplayInfo.account,
                                                    balance = assetDisplayInfo.amount,
                                                    fiatBalance = assetDisplayInfo.fiatValue,
                                                    actions = assetDisplayInfo.actions,
                                                    interestRate = assetDisplayInfo.interestRate
                                                )
                                            }
                                            is AssetDisplayInfo.DefiDisplayInfo -> {
                                                AssetDetailsItem.DefiDetailsInfo(
                                                    account = assetDisplayInfo.account,
                                                    balance = assetDisplayInfo.amount,
                                                    fiatBalance = assetDisplayInfo.fiatValue,
                                                    actions = assetDisplayInfo.actions,
                                                )
                                            }
                                        }
                                    },
                                    isAddedToWatchlist = accountInfo.isAddedToWatchlist
                                )
                                is AssetInformation.NonTradeable -> CoinViewViewState.ShowNonTradeableAccount(
                                    accountInfo.isAddedToWatchlist
                                )
                            },
                            assetInformation = accountInfo,
                            asset = intent.asset,
                            isAddedToWatchlist = accountInfo.isAddedToWatchlist
                        )
                    )

                    if (accountInfo is AssetInformation.AccountsInfo) {
                        process(
                            CoinViewIntent.LoadQuickActions(
                                accountInfo.totalCryptoBalance, accountInfo.accountsList.map { it.account },
                                intent.asset
                            )
                        )
                    }
                },
                onError = {
                    process(CoinViewIntent.UpdateErrorState(CoinViewError.WalletLoadError))
                }
            )
}
