package piuk.blockchain.android.ui.coinview.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.eth.MultiChainAccount
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.core.asset.domain.AssetService
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.recurringbuy.domain.RecurringBuyState
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.core.watchlist.domain.model.WatchlistToggle
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.doOnData
import com.blockchain.data.doOnError
import com.blockchain.data.map
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.toFormattedDateWithoutYear
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.github.mikephil.charting.data.Entry
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.text.DecimalFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.ui.coinview.domain.GetAccountActionsUseCase
import piuk.blockchain.android.ui.coinview.domain.GetAssetPriceUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadAssetAccountsUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadAssetRecurringBuysUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadQuickActionsUseCase
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetDetail
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickActions
import piuk.blockchain.android.ui.coinview.domain.model.isInterestAccount
import piuk.blockchain.android.ui.coinview.domain.model.isStakingAccount
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountState.Available
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountState.Unavailable
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountsHeaderState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState.Data.CoinviewRecurringBuyState
import timber.log.Timber

class CoinviewViewModel(
    walletModeService: WalletModeService,
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val labels: DefaultLabels,

    private val getAssetPriceUseCase: GetAssetPriceUseCase,

    private val watchlistService: WatchlistService,
    private val loadAssetAccountsUseCase: LoadAssetAccountsUseCase,
    private val getAccountActionsUseCase: GetAccountActionsUseCase,

    private val loadAssetRecurringBuysUseCase: LoadAssetRecurringBuysUseCase,

    private val loadQuickActionsUseCase: LoadQuickActionsUseCase,
    private val assetService: AssetService
) : MviViewModel<
    CoinviewIntent,
    CoinviewViewState,
    CoinviewModelState,
    CoinviewNavigationEvent,
    CoinviewArgs>(CoinviewModelState(walletMode = walletModeService.enabledWalletMode())) {

    companion object {
        const val SNACKBAR_MESSAGE_DURATION: Long = 3000L
    }

    private var loadPriceDataJob: Job? = null
    private var accountActionsJob: Job? = null
    private var loadWatchlistJob: Job? = null
    private var loadAccountsJob: Job? = null
    private var loadQuickActionsJob: Job? = null
    private var loadRecurringBuyJob: Job? = null
    private var loadAssetInfoJob: Job? = null
    private var snackbarMessageJob: Job? = null

    private val fiatCurrency: FiatCurrency
        get() = currencyPrefs.selectedFiatCurrency

    private val defaultTimeSpan = HistoricalTimeSpan.DAY

    override fun viewCreated(args: CoinviewArgs) {
        (coincore[args.networkTicker] as? CryptoAsset)?.let { asset ->
            updateState {
                it.copy(
                    asset = asset
                )
            }
        } ?: Timber.e("asset ${args.networkTicker} not found")

        args.recurringBuyId?.let {
            navigate(CoinviewNavigationEvent.ShowRecurringBuySheet(it))
        }
    }

    override fun reduce(state: CoinviewModelState): CoinviewViewState = state.run {
        CoinviewViewState(
            asset = reduceAsset(this),
            tradeable = reduceAssetTradeable(this),
            assetPrice = reduceAssetPrice(this),
            watchlist = reduceWatchlist(this),
            totalBalance = reduceTotalBalance(this),
            accounts = reduceAccounts(this),
            centerQuickAction = reduceCenterQuickActions(this),
            recurringBuys = reduceRecurringBuys(this),
            bottomQuickAction = reduceBottomQuickActions(this),
            assetInfo = reduceAssetInfo(this),
            snackbarError = reduceSnackbarError(this)
        )
    }

    private fun reduceAsset(state: CoinviewModelState): CoinviewAssetState = state.run {
        if (asset == null) {
            CoinviewAssetState.Error
        } else {
            CoinviewAssetState.Data(asset.currency)
        }
    }

    private fun reduceAssetTradeable(state: CoinviewModelState): CoinviewAssetTradeableState = state.run {
        if (isTradeableAsset == false) {
            check(asset != null) { "asset not initialized" }

            CoinviewAssetTradeableState.NonTradeable(
                assetName = asset.currency.name,
                assetTicker = asset.currency.networkTicker
            )
        } else {
            CoinviewAssetTradeableState.Tradeable
        }
    }

    private fun reduceAssetPrice(state: CoinviewModelState): CoinviewPriceState = state.run {
        when (assetPriceHistory) {
            DataResource.Loading -> {
                CoinviewPriceState.Loading
            }

            is DataResource.Error -> {
                CoinviewPriceState.Error
            }

            is DataResource.Data -> {
                // price, priceChange, percentChange
                // will contain values from interactiveAssetPrice to correspond with user interaction

                // intervalName will be empty if user is interacting with the chart

                check(asset != null) { "asset not initialized" }

                with(assetPriceHistory.data) {
                    CoinviewPriceState.Data(
                        assetName = asset.currency.name,
                        assetLogo = asset.currency.logo,
                        fiatSymbol = fiatCurrency.symbol,
                        price = (interactiveAssetPrice ?: priceDetail)
                            .price.toStringWithSymbol(),
                        priceChange = (interactiveAssetPrice ?: priceDetail)
                            .changeDifference.toStringWithSymbol(),
                        percentChange = (interactiveAssetPrice ?: priceDetail).percentChange,
                        intervalName = if (interactiveAssetPrice != null) R.string.empty else
                            when ((priceDetail).timeSpan) {
                                HistoricalTimeSpan.DAY -> R.string.coinview_price_day
                                HistoricalTimeSpan.WEEK -> R.string.coinview_price_week
                                HistoricalTimeSpan.MONTH -> R.string.coinview_price_month
                                HistoricalTimeSpan.YEAR -> R.string.coinview_price_year
                                HistoricalTimeSpan.ALL_TIME -> R.string.coinview_price_all
                            },
                        chartData = when {
                            isChartDataLoading &&
                                requestedTimeSpan != null &&
                                priceDetail.timeSpan != requestedTimeSpan -> {
                                // show chart loading when data is loading and a new timespan is selected
                                CoinviewPriceState.Data.CoinviewChartState.Loading
                            }
                            else -> CoinviewPriceState.Data.CoinviewChartState.Data(
                                historicRates.map { point ->
                                    ChartEntry(
                                        point.timestamp.toFloat(),
                                        point.rate.toFloat()
                                    )
                                }
                            )
                        },
                        selectedTimeSpan = requestedTimeSpan ?: (interactiveAssetPrice ?: priceDetail).timeSpan
                    )
                }
            }
        }
    }

    private fun reduceWatchlist(state: CoinviewModelState): CoinviewWatchlistState = state.run {
        when {
            // not supported for non custodial
            walletMode == WalletMode.NON_CUSTODIAL_ONLY -> {
                CoinviewWatchlistState.NotSupported
            }

            watchlist is DataResource.Loading -> {
                CoinviewWatchlistState.Loading
            }

            watchlist is DataResource.Error -> {
                CoinviewWatchlistState.Error
            }

            watchlist is DataResource.Data -> {
                CoinviewWatchlistState.Data(
                    isInWatchlist = watchlist.data,
                )
            }

            else -> {
                CoinviewWatchlistState.Loading
            }
        }
    }

    private fun reduceTotalBalance(state: CoinviewModelState): CoinviewTotalBalanceState = state.run {
        when {
            // not supported for non custodial
            walletMode == WalletMode.NON_CUSTODIAL_ONLY -> {
                CoinviewTotalBalanceState.NotSupported
            }

            assetDetail is DataResource.Loading -> {
                CoinviewTotalBalanceState.Loading
            }

            assetDetail is DataResource.Error -> {
                CoinviewTotalBalanceState.Error
            }

            assetDetail is DataResource.Data -> {
                check(asset != null) { "asset not initialized" }

                with(assetDetail.data) {
                    check(totalBalance.totalCryptoBalance.containsKey(AssetFilter.All)) { "balance not initialized" }

                    CoinviewTotalBalanceState.Data(
                        assetName = asset.currency.name,
                        totalFiatBalance = totalBalance.totalFiatBalance.toStringWithSymbol(),
                        totalCryptoBalance = totalBalance.totalCryptoBalance[AssetFilter.All]?.toStringWithSymbol()
                            .orEmpty()
                    )
                }
            }

            else -> {
                CoinviewTotalBalanceState.Loading
            }
        }
    }

    private fun reduceAccounts(state: CoinviewModelState): CoinviewAccountsState = state.run {
        when {
            isTradeableAsset == false -> {
                CoinviewAccountsState.NotSupported
            }

            assetDetail is DataResource.Loading -> {
                CoinviewAccountsState.Loading
            }

            assetDetail is DataResource.Error -> {
                CoinviewAccountsState.Error
            }

            assetDetail is DataResource.Data && assetDetail.data is CoinviewAssetDetail.Tradeable -> {
                check(asset != null) { "reduceAccounts - asset not initialized" }

                with(assetDetail.data as CoinviewAssetDetail.Tradeable) {
                    CoinviewAccountsState.Data(
                        style = when (accounts) {
                            is CoinviewAccounts.Universal,
                            is CoinviewAccounts.Custodial -> CoinviewAccountsStyle.Simple
                            is CoinviewAccounts.Defi -> CoinviewAccountsStyle.Boxed
                        },
                        header = when (accounts) {
                            is CoinviewAccounts.Universal,
                            is CoinviewAccounts.Custodial -> CoinviewAccountsHeaderState.ShowHeader(
                                TextValue.IntResValue(R.string.coinview_accounts_label)
                            )
                            is CoinviewAccounts.Defi -> CoinviewAccountsHeaderState.NoHeader
                        },
                        accounts = accounts.accounts.map { cvAccount ->
                            val account: CryptoAccount = cvAccount.account.let { blockchainAccount ->
                                when (blockchainAccount) {
                                    is CryptoAccount -> blockchainAccount
                                    is AccountGroup -> blockchainAccount.selectFirstAccount()
                                    else -> throw IllegalStateException(
                                        "Unsupported account type for asset details ${cvAccount.account}"
                                    )
                                }
                            }

                            when (cvAccount.isEnabled) {
                                true -> {
                                    when (cvAccount) {
                                        is CoinviewAccount.Universal ->
                                            makeAvailableUniversalAccount(cvAccount, account, asset)
                                        is CoinviewAccount.Custodial.Trading ->
                                            makeAvailableTradingAccount(cvAccount, asset)
                                        is CoinviewAccount.Custodial.Interest ->
                                            makeAvailableInterestAccount(cvAccount, asset)
                                        is CoinviewAccount.Custodial.Staking ->
                                            makeAvailableStakingAccount(cvAccount, asset)
                                        is CoinviewAccount.PrivateKey ->
                                            makeAvailablePrivateKeyAccount(cvAccount, account, asset)
                                    }
                                }
                                false -> {
                                    when (cvAccount) {
                                        is CoinviewAccount.Universal ->
                                            makeUnavailableUniversalAccount(cvAccount, account, asset)
                                        is CoinviewAccount.Custodial.Trading ->
                                            makeUnavailableTradingAccount(cvAccount, asset)
                                        is CoinviewAccount.Custodial.Interest ->
                                            makeUnavailableInterestAccount(cvAccount)
                                        is CoinviewAccount.Custodial.Staking ->
                                            makeUnavailableStakingAccount(cvAccount)
                                        is CoinviewAccount.PrivateKey ->
                                            makeUnavailablePrivateKeyAccount(cvAccount, account)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            else -> {
                CoinviewAccountsState.Loading
            }
        }
    }

    private fun makeUnavailablePrivateKeyAccount(
        cvAccount: CoinviewAccount,
        account: CryptoAccount
    ) = Unavailable(
        cvAccount = cvAccount,
        title = account.currency.name,
        subtitle = TextValue.IntResValue(R.string.coinview_nc_desc),
        logo = LogoSource.Remote(account.currency.logo)
    )

    private fun makeUnavailableStakingAccount(cvAccount: CoinviewAccount.Custodial.Staking) =
        Unavailable(
            cvAccount = cvAccount,
            title = labels.getDefaultStakingWalletLabel(),
            subtitle = TextValue.IntResValue(
                R.string.coinview_interest_no_balance,
                listOf(DecimalFormat("0.#").format(cvAccount.stakingRate))
            ),
            logo = LogoSource.Resource(R.drawable.ic_staking_account_indicator)
        )

    private fun makeUnavailableInterestAccount(cvAccount: CoinviewAccount.Custodial.Interest) =
        Unavailable(
            cvAccount = cvAccount,
            title = labels.getDefaultInterestWalletLabel(),
            subtitle = TextValue.IntResValue(
                R.string.coinview_interest_no_balance,
                listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
            ),
            logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator)
        )

    private fun makeUnavailableTradingAccount(
        cvAccount: CoinviewAccount,
        asset: CryptoAsset
    ) = Unavailable(
        cvAccount = cvAccount,
        title = labels.getDefaultTradingWalletLabel(),
        subtitle = TextValue.IntResValue(
            R.string.coinview_c_unavailable_desc,
            listOf(asset.currency.name)
        ),
        logo = LogoSource.Resource(R.drawable.ic_custodial_account_indicator)
    )

    private fun makeUnavailableUniversalAccount(
        cvAccount: CoinviewAccount.Universal,
        account: CryptoAccount,
        asset: CryptoAsset
    ) = Unavailable(
        cvAccount = cvAccount,
        title = when (cvAccount.filter) {
            AssetFilter.Trading -> labels.getDefaultTradingWalletLabel()
            AssetFilter.Interest -> labels.getDefaultInterestWalletLabel()
            AssetFilter.Staking -> labels.getDefaultStakingWalletLabel()
            AssetFilter.NonCustodial -> account.label
            else -> error(
                "Filer ${cvAccount.filter} not supported for account label"
            )
        },
        subtitle = when (cvAccount.filter) {
            AssetFilter.Trading -> {
                TextValue.IntResValue(
                    R.string.coinview_c_unavailable_desc,
                    listOf(asset.currency.name)
                )
            }
            AssetFilter.Interest -> {
                TextValue.IntResValue(
                    R.string.coinview_interest_no_balance,
                    listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
                )
            }
            AssetFilter.Staking -> {
                TextValue.IntResValue(
                    R.string.coinview_interest_no_balance,
                    listOf(DecimalFormat("0.#").format(cvAccount.stakingRate))
                )
            }
            AssetFilter.NonCustodial -> {
                TextValue.IntResValue(R.string.coinview_nc_desc)
            }
            else -> error("${cvAccount.filter} Not a supported filter")
        },
        logo = LogoSource.Resource(
            when (cvAccount.filter) {
                AssetFilter.Trading -> {
                    R.drawable.ic_custodial_account_indicator
                }
                AssetFilter.Interest -> {
                    R.drawable.ic_interest_account_indicator
                }
                AssetFilter.NonCustodial -> {
                    R.drawable.ic_non_custodial_account_indicator
                }
                else -> error("${cvAccount.filter} Not a supported filter")
            }
        )
    )

    private fun makeAvailablePrivateKeyAccount(
        cvAccount: CoinviewAccount,
        account: CryptoAccount,
        asset: CryptoAsset
    ) = Available(
        cvAccount = cvAccount,
        title = account.label,
        subtitle = TextValue.StringValue(account.currency.displayTicker),
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        logo = LogoSource.Remote(account.currency.logo),
        assetColor = asset.currency.colour
    )

    private fun makeAvailableStakingAccount(
        cvAccount: CoinviewAccount.Custodial.Staking,
        asset: CryptoAsset
    ) = Available(
        cvAccount = cvAccount,
        title = labels.getDefaultStakingWalletLabel(),
        subtitle = TextValue.IntResValue(
            R.string.coinview_interest_with_balance,
            listOf(DecimalFormat("0.#").format(cvAccount.stakingRate))
        ),
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        logo = LogoSource.Resource(R.drawable.ic_staking_account_indicator),
        assetColor = asset.currency.colour
    )

    private fun makeAvailableInterestAccount(
        cvAccount: CoinviewAccount.Custodial.Interest,
        asset: CryptoAsset
    ) = Available(
        cvAccount = cvAccount,
        title = labels.getDefaultInterestWalletLabel(),
        subtitle = TextValue.IntResValue(
            R.string.coinview_interest_with_balance,
            listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
        ),
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator),
        assetColor = asset.currency.colour
    )

    private fun makeAvailableTradingAccount(
        cvAccount: CoinviewAccount,
        asset: CryptoAsset
    ) = Available(
        cvAccount = cvAccount,
        title = labels.getDefaultTradingWalletLabel(),
        subtitle = TextValue.IntResValue(R.string.coinview_c_available_desc),
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        logo = LogoSource.Resource(R.drawable.ic_custodial_account_indicator),
        assetColor = asset.currency.colour
    )

    private fun makeAvailableUniversalAccount(
        cvAccount: CoinviewAccount.Universal,
        account: CryptoAccount,
        asset: CryptoAsset
    ) = Available(
        cvAccount = cvAccount,
        title = when (cvAccount.filter) {
            AssetFilter.Trading -> labels.getDefaultTradingWalletLabel()
            AssetFilter.Interest -> labels.getDefaultInterestWalletLabel()
            AssetFilter.Staking -> labels.getDefaultStakingWalletLabel()
            AssetFilter.NonCustodial -> account.label
            else -> error(
                "Filter ${cvAccount.filter} not supported for account label"
            )
        },
        subtitle = when (cvAccount.filter) {
            AssetFilter.Trading -> {
                TextValue.IntResValue(R.string.coinview_c_available_desc)
            }
            AssetFilter.Interest -> {
                TextValue.IntResValue(
                    R.string.coinview_interest_with_balance,
                    listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
                )
            }
            AssetFilter.Staking -> {
                TextValue.IntResValue(
                    R.string.coinview_interest_with_balance,
                    listOf(DecimalFormat("0.#").format(cvAccount.stakingRate))
                )
            }
            AssetFilter.NonCustodial -> {
                if (account is MultiChainAccount) {
                    TextValue.IntResValue(
                        R.string.coinview_multi_nc_desc,
                        listOf(account.l1Network.networkName)
                    )
                } else {
                    TextValue.IntResValue(R.string.coinview_nc_desc)
                }
            }
            else -> error("${cvAccount.filter} Not a supported filter")
        },
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        logo = LogoSource.Resource(
            when (cvAccount.filter) {
                AssetFilter.Trading -> {
                    R.drawable.ic_custodial_account_indicator
                }
                AssetFilter.Interest -> {
                    R.drawable.ic_interest_account_indicator
                }
                AssetFilter.Staking -> {
                    R.drawable.ic_staking_account_indicator
                }
                AssetFilter.NonCustodial -> {
                    R.drawable.ic_non_custodial_account_indicator
                }
                else -> error("${cvAccount.filter} Not a supported filter")
            }
        ),
        assetColor = asset.currency.colour
    )

    private fun reduceRecurringBuys(state: CoinviewModelState): CoinviewRecurringBuysState = state.run {
        when {
            // not supported for non custodial
            isTradeableAsset == false || walletMode == WalletMode.NON_CUSTODIAL_ONLY -> {
                CoinviewRecurringBuysState.NotSupported
            }

            recurringBuys is DataResource.Loading -> {
                CoinviewRecurringBuysState.Loading
            }

            recurringBuys is DataResource.Error -> {
                CoinviewRecurringBuysState.Error
            }

            recurringBuys is DataResource.Data -> {
                check(asset != null) { "asset not initialized" }

                with(recurringBuys.data) {
                    when {
                        data.isEmpty() && isAvailableForTrading -> {
                            CoinviewRecurringBuysState.Upsell
                        }

                        data.isEmpty() && isAvailableForTrading.not() -> {
                            CoinviewRecurringBuysState.NotSupported
                        }

                        else -> CoinviewRecurringBuysState.Data(
                            data.map { recurringBuy ->
                                CoinviewRecurringBuyState(
                                    id = recurringBuy.id,
                                    description = TextValue.IntResValue(
                                        R.string.dashboard_recurring_buy_item_title_1,
                                        listOf(
                                            recurringBuy.amount.toStringWithSymbol(),
                                            recurringBuy.recurringBuyFrequency.toHumanReadableRecurringBuy()
                                        )
                                    ),

                                    status = if (recurringBuy.state ==
                                        RecurringBuyState.ACTIVE
                                    ) {
                                        TextValue.IntResValue(
                                            R.string.dashboard_recurring_buy_item_label,
                                            listOf(recurringBuy.nextPaymentDate.toFormattedDateWithoutYear())
                                        )
                                    } else {
                                        TextValue.IntResValue(
                                            R.string.dashboard_recurring_buy_item_label_error
                                        )
                                    },

                                    assetColor = asset.currency.colour
                                )
                            }
                        )
                    }
                }
            }

            else -> {
                CoinviewRecurringBuysState.Loading
            }
        }
    }

    private fun reduceCenterQuickActions(state: CoinviewModelState): CoinviewCenterQuickActionsState = state.run {
        when {
            isTradeableAsset == false -> {
                CoinviewCenterQuickActionsState.NotSupported
            }

            quickActions is DataResource.Loading -> {
                CoinviewCenterQuickActionsState.Loading
            }

            quickActions is DataResource.Error -> {
                CoinviewCenterQuickActionsState.Data(
                    center = CoinviewQuickAction.None.toViewState()
                )
            }

            quickActions is DataResource.Data -> {
                with(quickActions.data) {
                    CoinviewCenterQuickActionsState.Data(
                        center = center.toViewState()
                    )
                }
            }

            else -> {
                CoinviewCenterQuickActionsState.Loading
            }
        }
    }

    private fun reduceBottomQuickActions(state: CoinviewModelState): CoinviewBottomQuickActionsState = state.run {
        when {
            isTradeableAsset == false -> {
                CoinviewBottomQuickActionsState.NotSupported
            }

            quickActions is DataResource.Loading -> {
                CoinviewBottomQuickActionsState.Loading
            }

            quickActions is DataResource.Error -> {
                CoinviewBottomQuickActionsState.Data(
                    start = CoinviewQuickAction.None.toViewState(),
                    end = CoinviewQuickAction.None.toViewState()
                )
            }

            quickActions is DataResource.Data -> {
                with(quickActions.data) {
                    CoinviewBottomQuickActionsState.Data(
                        start = bottomStart.toViewState(),
                        end = bottomEnd.toViewState()
                    )
                }
            }

            else -> {
                CoinviewBottomQuickActionsState.Loading
            }
        }
    }

    private fun reduceAssetInfo(state: CoinviewModelState): CoinviewAssetInfoState = state.run {
        when (assetInfo) {
            DataResource.Loading -> {
                CoinviewAssetInfoState.Loading
            }

            is DataResource.Error -> {
                CoinviewAssetInfoState.Error
            }

            is DataResource.Data -> {
                require(asset != null) { "asset not initialized" }

                with(assetInfo.data) {
                    CoinviewAssetInfoState.Data(
                        assetName = asset.currency.name,
                        description = if (description.isEmpty().not()) description else null,
                        website = if (website.isEmpty().not()) website else null,
                    )
                }
            }
        }
    }

    private fun reduceSnackbarError(state: CoinviewModelState): CoinviewSnackbarAlertState = state.run {
        when (state.error) {
            CoinviewError.AccountsLoadError -> CoinviewSnackbarAlertState.AccountsLoadError
            CoinviewError.ActionsLoadError -> CoinviewSnackbarAlertState.ActionsLoadError
            CoinviewError.WatchlistToggleError -> CoinviewSnackbarAlertState.WatchlistToggleError
            CoinviewError.None -> CoinviewSnackbarAlertState.None
        }.also {
            // reset to None
            if (it != CoinviewSnackbarAlertState.None) {
                snackbarMessageJob?.cancel()
                snackbarMessageJob = viewModelScope.launch {
                    delay(SNACKBAR_MESSAGE_DURATION)

                    updateState {
                        it.copy(error = CoinviewError.None)
                    }
                }
            }
        }
    }

    override suspend fun handleIntent(modelState: CoinviewModelState, intent: CoinviewIntent) {
        when (intent) {
            is CoinviewIntent.LoadAllData -> {
                check(modelState.asset != null) { "LoadAllData asset not initialized" }
                onIntent(CoinviewIntent.LoadPriceData)
                onIntent(CoinviewIntent.LoadAccountsData)
                onIntent(CoinviewIntent.LoadWatchlistData)
                onIntent(CoinviewIntent.LoadRecurringBuysData)
                onIntent(CoinviewIntent.LoadAssetInfo)
            }

            CoinviewIntent.LoadPriceData -> {
                check(modelState.asset != null) { "LoadPriceData asset not initialized" }

                loadPriceData(
                    asset = modelState.asset,
                    requestedTimeSpan = (modelState.assetPriceHistory as? DataResource.Data)
                        ?.data?.priceDetail?.timeSpan ?: defaultTimeSpan
                )
            }

            CoinviewIntent.LoadWatchlistData -> {
                require(modelState.asset != null) { "asset not initialized" }

                loadWatchlistData(
                    asset = modelState.asset,
                )
            }

            CoinviewIntent.LoadAccountsData -> {
                check(modelState.asset != null) { "LoadAccountsData asset not initialized" }

                loadAccountsData(
                    asset = modelState.asset,
                )
            }

            CoinviewIntent.LoadRecurringBuysData -> {
                check(modelState.asset != null) { "LoadRecurringBuysData asset not initialized" }

                loadRecurringBuysData(
                    asset = modelState.asset,
                )
            }

            is CoinviewIntent.LoadQuickActions -> {
                check(modelState.asset != null) { "LoadQuickActions asset not initialized" }

                loadQuickActionsData(
                    asset = modelState.asset,
                    accounts = intent.accounts,
                    totalBalance = intent.totalBalance
                )
            }

            CoinviewIntent.LoadAssetInfo -> {
                check(modelState.asset != null) { "LoadAssetInfo asset not initialized" }

                loadAssetInformation(
                    asset = modelState.asset,
                )
            }

            is CoinviewIntent.UpdatePriceForChartSelection -> {
                check(
                    modelState.assetPriceHistory is DataResource.Data
                ) { "UpdatePriceForChartSelection price data not initialized" }

                updatePriceForChartSelection(
                    entry = intent.entry,
                    assetPriceHistory = modelState.assetPriceHistory.data
                )
            }

            is CoinviewIntent.ResetPriceSelection -> {
                resetPriceSelection()
            }

            is CoinviewIntent.NewTimeSpanSelected -> {
                check(modelState.asset != null) { "NewTimeSpanSelected asset not initialized" }

                updateState { it.copy(requestedTimeSpan = intent.timeSpan) }

                loadPriceData(
                    asset = modelState.asset,
                    requestedTimeSpan = intent.timeSpan,
                )
            }

            CoinviewIntent.ToggleWatchlist -> {
                check(modelState.asset != null) { "asset not initialized" }
                check(modelState.watchlist is DataResource.Data) { "watchlist not initialized" }

                updateWatchlist(
                    asset = modelState.asset,
                    toggle = if (modelState.watchlist.data) WatchlistToggle.REMOVE else WatchlistToggle.ADD
                )
            }

            is CoinviewIntent.AccountSelected -> {
                check(modelState.asset != null) { "AccountSelected asset not initialized" }
                require(intent.account.cryptoBalance is DataResource.Data)
                require(intent.account.fiatBalance is DataResource.Data)
                handleAccountSelected(
                    account = intent.account,
                    asset = modelState.asset
                )
            }

            is CoinviewIntent.AccountExplainerAcknowledged -> {
                check(modelState.accounts != null) { "AccountExplainerAcknowledged accounts not initialized" }

                val cvAccount = modelState.accounts!!.accounts.first { it.account == intent.account }
                val balance = cvAccount.cryptoBalance as? DataResource.Data ?: return
                val fiatBalance = cvAccount.fiatBalance as? DataResource.Data ?: return
                navigate(
                    CoinviewNavigationEvent.ShowAccountActions(
                        cvAccount = cvAccount,
                        interestRate = cvAccount.interestRate(),
                        stakingRate = cvAccount.stakingRate(),
                        actions = intent.actions,
                        cryptoBalance = balance.data,
                        fiatBalance = fiatBalance.data,
                    )
                )
            }

            is CoinviewIntent.AccountActionSelected -> {
                require(modelState.asset != null) { "AccountActionSelected asset not initialized" }
                require(modelState.accounts != null) { "AccountActionSelected accounts not initialized" }

                val cvAccount = modelState.accounts!!.accounts.first { it.account == intent.account }

                handleAccountActionSelected(
                    account = cvAccount,
                    asset = modelState.asset,
                    action = intent.action
                )
            }

            is CoinviewIntent.NoBalanceUpsell -> {
                require(modelState.accounts != null) { "NoBalanceUpsell accounts not initialized" }

                val cvAccount = modelState.accounts!!.accounts.first { it.account == intent.account }

                navigate(
                    CoinviewNavigationEvent.ShowNoBalanceUpsell(
                        cvAccount,
                        intent.action,
                        true
                    )
                )
            }

            CoinviewIntent.LockedAccountSelected -> {
                navigate(
                    CoinviewNavigationEvent.ShowKycUpgrade
                )
            }

            CoinviewIntent.RecurringBuysUpsell -> {
                require(modelState.asset != null) { "RecurringBuysUpsell asset not initialized" }

                navigate(
                    CoinviewNavigationEvent.NavigateToRecurringBuyUpsell(modelState.asset)
                )
            }

            is CoinviewIntent.ShowRecurringBuyDetail -> {
                navigate(
                    CoinviewNavigationEvent.ShowRecurringBuyInfo(
                        recurringBuyId = intent.recurringBuyId
                    )
                )
            }

            is CoinviewIntent.QuickActionSelected -> {
                require(modelState.asset != null) { "QuickActionSelected asset not initialized" }

                when (intent.quickAction) {
                    is CoinviewQuickAction.Buy -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToBuy(
                                asset = modelState.asset
                            )
                        )
                    }

                    is CoinviewQuickAction.Sell -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToSell(
                                cvAccount = modelState.actionableAccount()
                            )
                        )
                    }

                    is CoinviewQuickAction.Send -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToSend(
                                cvAccount = modelState.actionableAccount()
                            )
                        )
                    }

                    is CoinviewQuickAction.Receive -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToReceive(
                                cvAccount = modelState.actionableAccount(isPositiveBalanceRequired = false),
                                isBuyReceive = (modelState.quickActions.map { it.canBuy() } as? DataResource.Data)
                                    ?.data ?: false,
                                isSendReceive = (modelState.quickActions.map { it.canSend() } as? DataResource.Data)
                                    ?.data ?: false
                            )
                        )
                    }

                    is CoinviewQuickAction.Swap -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToSwap(
                                cvAccount = modelState.actionableAccount()
                            )
                        )
                    }

                    CoinviewQuickAction.None -> error("CoinviewQuickAction.None action doesn't have an action")
                }
            }

            CoinviewIntent.ContactSupport -> {
                navigate(CoinviewNavigationEvent.NavigateToSupport)
            }

            CoinviewIntent.VisitAssetWebsite -> {
                check(modelState.assetInfo is DataResource.Data) { "assetInfo not initialized" }
                navigate(CoinviewNavigationEvent.OpenAssetWebsite(modelState.assetInfo.data.website))
            }

            is CoinviewIntent.LaunchStakingDepositFlow ->
                navigate(CoinviewNavigationEvent.NavigateToStakingDeposit(getStakingAccount(modelState.accounts)))

            is CoinviewIntent.LaunchStakingActivity ->
                navigate(CoinviewNavigationEvent.NavigateToActivity(getStakingAccount(modelState.accounts)))
        }
    }

    private fun getStakingAccount(accounts: CoinviewAccounts?): CoinviewAccount {
        require(accounts != null) { "getStakingAccount - accounts not initialized" }
        require(accounts.accounts.filterIsInstance<CoinviewAccount.Custodial.Staking>().isNotEmpty()) {
            "getStakingAccount no staking account source found"
        }

        return modelState.accounts!!.accounts.first { it is CoinviewAccount.Custodial.Staking }
    }

    // //////////////////////
    // Prices
    private fun loadPriceData(
        asset: CryptoAsset,
        requestedTimeSpan: HistoricalTimeSpan
    ) {
        loadPriceDataJob?.cancel()
        loadPriceDataJob = viewModelScope.launch {
            getAssetPriceUseCase(
                asset = asset, timeSpan = requestedTimeSpan, fiatCurrency = fiatCurrency
            ).collectLatest { dataResource ->
                when (dataResource) {
                    DataResource.Loading -> {
                        updateState {
                            it.copy(
                                isChartDataLoading = true,
                                assetPriceHistory = if (it.assetPriceHistory is DataResource.Data) {
                                    it.assetPriceHistory
                                } else {
                                    dataResource
                                }
                            )
                        }
                    }

                    is DataResource.Error -> {
                        updateState {
                            it.copy(
                                isChartDataLoading = false,
                                assetPriceHistory = dataResource,
                            )
                        }
                    }

                    is DataResource.Data -> {
                        if (dataResource.data.historicRates.isEmpty()) {
                            updateState {
                                it.copy(
                                    isChartDataLoading = false,
                                    assetPriceHistory = DataResource.Error(Exception("no historicRates"))
                                )
                            }
                        } else {
                            updateState {
                                it.copy(
                                    isChartDataLoading = false,
                                    assetPriceHistory = dataResource,
                                    requestedTimeSpan = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Build a [CoinviewModelState.interactiveAssetPrice] based on the [entry] selected
     * to update the price information with
     */
    private fun updatePriceForChartSelection(
        entry: Entry,
        assetPriceHistory: CoinviewAssetPriceHistory,
    ) {
        val historicRates = assetPriceHistory.historicRates
        historicRates.firstOrNull { it.timestamp.toFloat() == entry.x }?.let { selectedHistoricalRate ->
            val firstForPeriod = historicRates.first()
            val difference = selectedHistoricalRate.rate - firstForPeriod.rate

            val percentChange = (difference / firstForPeriod.rate)

            val changeDifference = Money.fromMajor(fiatCurrency, difference.toBigDecimal())

            updateState {
                it.copy(
                    interactiveAssetPrice = CoinviewAssetPrice(
                        price = Money.fromMajor(
                            fiatCurrency, selectedHistoricalRate.rate.toBigDecimal()
                        ),
                        timeSpan = assetPriceHistory.priceDetail.timeSpan,
                        changeDifference = changeDifference,
                        percentChange = percentChange
                    )
                )
            }
        }
    }

    /**
     * Reset [CoinviewModelState.interactiveAssetPrice] to update the price information with original value
     */
    private fun resetPriceSelection() {
        updateState { it.copy(interactiveAssetPrice = null) }
    }

    // //////////////////////
    // Watchlist
    private fun loadWatchlistData(asset: CryptoAsset) {
        loadWatchlistJob?.cancel()
        loadWatchlistJob = viewModelScope.launch {
            watchlistService.isAssetInWatchlist(asset.currency).collectLatest { dataResource ->
                updateState {
                    it.copy(
                        watchlist = if (dataResource is DataResource.Loading && it.watchlist is DataResource.Data) {
                            it.watchlist
                        } else {
                            dataResource
                        }
                    )
                }
            }
        }
    }

    private fun updateWatchlist(asset: CryptoAsset, toggle: WatchlistToggle) {
        viewModelScope.launch {
            watchlistService.updateWatchlist(
                asset = asset.currency,
                toggle = toggle
            ).let { dataResource ->
                when (dataResource) {
                    is DataResource.Error -> {
                        updateState {
                            it.copy(error = CoinviewError.WatchlistToggleError)
                        }
                    }
                    is DataResource.Data -> {
                        loadWatchlistData(
                            asset = asset,
                        )
                    }
                    DataResource.Loading -> {
                        // n/a
                    }
                }
            }
        }
    }

    // //////////////////////
    // Accounts
    /**
     * Loads accounts and total balance
     */
    private fun loadAccountsData(asset: CryptoAsset) {
        loadAccountsJob?.cancel()
        loadAccountsJob = viewModelScope.launch {
            loadAssetAccountsUseCase(asset = asset).collectLatest { dataResource ->

                updateState {
                    it.copy(
                        assetDetail = if (dataResource is DataResource.Loading && it.assetDetail is DataResource.Data) {
                            // if data is present already - don't show loading
                            it.assetDetail
                        } else {
                            dataResource
                        },
                        // on failure - fail quick actions too
                        // on data - load quick actions /see below
                        quickActions = if (dataResource is DataResource.Error) {
                            DataResource.Error(dataResource.error)
                        } else {
                            it.quickActions
                        },
                        error = if (dataResource is DataResource.Error) {
                            CoinviewError.AccountsLoadError
                        } else {
                            it.error
                        }
                    )
                }

                if (dataResource is DataResource.Data) {
                    when (dataResource.data) {
                        is CoinviewAssetDetail.Tradeable -> {
                            // now that we got accounts and it's a tradeable asset
                            // -> get quick actions
                            with(dataResource.data as CoinviewAssetDetail.Tradeable) {
                                onIntent(
                                    CoinviewIntent.LoadQuickActions(
                                        accounts = accounts,
                                        totalBalance = totalBalance
                                    )
                                )
                            }
                        }

                        is CoinviewAssetDetail.NonTradeable -> {
                            // cancel flows
                            loadAccountsJob?.cancel()
                            loadQuickActionsJob?.cancel()
                            loadRecurringBuyJob?.cancel()
                        }
                    }
                }
            }
        }
    }

    private fun handleAccountSelected(account: CoinviewAccount, asset: CryptoAsset) {
        accountActionsJob?.cancel()
        accountActionsJob = viewModelScope.launch {
            getAccountActionsUseCase(account)
                .doOnData { actions ->
                    getAccountActionsUseCase.getSeenAccountExplainerState(account).let { (hasSeen, markAsSeen) ->
                        if (hasSeen.not()) {
                            // show explainer
                            navigate(
                                CoinviewNavigationEvent.ShowAccountExplainer(
                                    cvAccount = account,
                                    networkTicker = asset.currency.networkTicker,
                                    interestRate = account.interestRate(),
                                    stakingRate = account.stakingRate(),
                                    actions = actions
                                )
                            )
                            markAsSeen()
                        } else {
                            navigate(
                                CoinviewNavigationEvent.ShowAccountActions(
                                    cvAccount = account,
                                    interestRate = account.interestRate(),
                                    stakingRate = account.stakingRate(),
                                    actions = actions,
                                    cryptoBalance = (account.cryptoBalance as DataResource.Data).data,
                                    fiatBalance = (account.fiatBalance as DataResource.Data).data
                                )
                            )
                        }
                    }
                }
                .doOnError {
                    updateState {
                        it.copy(error = CoinviewError.ActionsLoadError)
                    }
                }
        }
    }

    private fun CoinviewAccount.interestRate(): Double {
        val noInterestRate = 0.0
        return when (this) {
            is CoinviewAccount.Universal -> {
                if (filter == AssetFilter.Interest) {
                    interestRate
                } else {
                    noInterestRate
                }
            }
            is CoinviewAccount.Custodial.Interest -> {
                interestRate
            }
            else -> {
                noInterestRate
            }
        }
    }

    private fun CoinviewAccount.stakingRate(): Double {
        val noStakingRate = 0.0
        return when (this) {
            is CoinviewAccount.Universal -> {
                if (filter == AssetFilter.Staking) {
                    stakingRate
                } else {
                    noStakingRate
                }
            }
            is CoinviewAccount.Custodial.Staking -> {
                stakingRate
            }
            else -> {
                noStakingRate
            }
        }
    }

    private fun handleAccountActionSelected(
        account: CoinviewAccount,
        asset: CryptoAsset,
        action: AssetAction
    ) {
        when (action) {
            AssetAction.Send -> navigate(
                CoinviewNavigationEvent.NavigateToSend(
                    cvAccount = account
                )
            )

            AssetAction.Receive -> navigate(
                CoinviewNavigationEvent.NavigateToReceive(
                    cvAccount = account,
                    isBuyReceive = (modelState.quickActions.map { it.canBuy() } as? DataResource.Data)
                        ?.data ?: false,
                    isSendReceive = (modelState.quickActions.map { it.canSend() } as? DataResource.Data)
                        ?.data ?: false
                )
            )

            AssetAction.Sell -> navigate(
                CoinviewNavigationEvent.NavigateToSell(
                    cvAccount = account
                )
            )

            AssetAction.Buy -> navigate(
                CoinviewNavigationEvent.NavigateToBuy(
                    asset = asset
                )
            )

            AssetAction.Swap -> navigate(
                CoinviewNavigationEvent.NavigateToSwap(
                    cvAccount = account
                )
            )

            AssetAction.ViewActivity -> navigate(
                CoinviewNavigationEvent.NavigateToActivity(
                    cvAccount = account
                )
            )

            AssetAction.ViewStatement -> navigate(
                when {
                    account.isInterestAccount() -> {
                        CoinviewNavigationEvent.NavigateToInterestStatement(
                            cvAccount = account
                        )
                    }
                    account.isStakingAccount() -> {
                        CoinviewNavigationEvent.NavigateToStakingStatement(cvAccount = account)
                    }
                    else -> throw IllegalStateException("ViewStatement is not supported for account $account")
                }
            )

            AssetAction.InterestDeposit -> navigate(
                CoinviewNavigationEvent.NavigateToInterestDeposit(
                    cvAccount = account
                )
            )

            AssetAction.InterestWithdraw -> navigate(
                CoinviewNavigationEvent.NavigateToInterestWithdraw(
                    cvAccount = account
                )
            )

            AssetAction.StakingDeposit -> navigate(
                CoinviewNavigationEvent.NavigateToStakingDeposit(
                    cvAccount = account
                )
            )

            else -> throw IllegalStateException("Action $action is not supported in this flow")
        }
    }

    // //////////////////////
    // Recurring buys
    private fun loadRecurringBuysData(asset: CryptoAsset) {
        loadRecurringBuyJob?.cancel()
        loadRecurringBuyJob = viewModelScope.launch {
            loadAssetRecurringBuysUseCase(asset = asset).collectLatest { dataResource ->
                updateState {
                    it.copy(
                        recurringBuys = if (dataResource is DataResource.Loading &&
                            it.recurringBuys is DataResource.Data
                        ) {
                            it.recurringBuys
                        } else {
                            dataResource
                        }
                    )
                }
            }
        }
    }

    // //////////////////////
    // Quick actions
    private fun loadQuickActionsData(
        asset: CryptoAsset,
        accounts: CoinviewAccounts,
        totalBalance: CoinviewAssetTotalBalance
    ) {
        loadQuickActionsJob?.cancel()
        loadQuickActionsJob = viewModelScope.launch {
            loadQuickActionsUseCase(
                asset = asset,
                accounts = accounts,
                totalBalance = totalBalance
            ).collectLatest { dataResource ->
                updateState {
                    it.copy(
                        quickActions = if (dataResource is DataResource.Loading &&
                            it.quickActions is DataResource.Data
                        ) {
                            it.quickActions
                        } else {
                            dataResource
                        }
                    )
                }
            }
        }
    }

    private fun CoinviewQuickActions.canBuy(): Boolean {
        return actions.any { it == CoinviewQuickAction.Buy(true) }
    }

    private fun CoinviewQuickActions.canSend(): Boolean {
        return actions.any { it == CoinviewQuickAction.Send(true) }
    }

    // //////////////////////
    // Asset info
    private fun loadAssetInformation(asset: CryptoAsset) {
        loadAssetInfoJob?.cancel()
        loadAssetInfoJob = viewModelScope.launch {
            assetService.getAssetInformation(asset = asset.currency).collectLatest { dataResource ->
                updateState {
                    it.copy(
                        assetInfo = if (dataResource is DataResource.Loading &&
                            it.assetInfo is DataResource.Data
                        ) {
                            it.assetInfo
                        } else {
                            dataResource
                        }
                    )
                }
            }
        }
    }
}
