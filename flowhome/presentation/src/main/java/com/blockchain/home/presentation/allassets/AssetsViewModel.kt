package com.blockchain.home.presentation.allassets

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.anyError
import com.blockchain.data.anyLoading
import com.blockchain.data.dataOrElse
import com.blockchain.data.doOnError
import com.blockchain.data.filter
import com.blockchain.data.flatMap
import com.blockchain.data.getFirstError
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.extensions.minus
import com.blockchain.extensions.replace
import com.blockchain.home.domain.AssetBalance
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.domain.FiltersService
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.home.domain.SingleAccountBalance
import com.blockchain.home.domain.isSmallBalance
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.balance.WalletBalance
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.utils.CurrentTimeProvider
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class AssetsViewModel(
    private val homeAccountsService: HomeAccountsService,
    private val currencyPrefs: CurrencyPrefs,
    private val assetCatalogue: AssetCatalogue,
    private val exchangeRates: ExchangeRatesDataManager,
    private val walletModeService: WalletModeService,
    private val filterService: FiltersService,
    private val coincore: Coincore,
    private val dispatcher: CoroutineDispatcher,
) : MviViewModel<AssetsIntent, AssetsViewState, AssetsModelState, HomeNavEvent, ModelConfigArgs.NoArgs>(
    AssetsModelState(walletMode = WalletMode.CUSTODIAL, userFiat = currencyPrefs.selectedFiatCurrency)
) {
    private var accountsJob: Job? = null
    private var fundsLocksJob: Job? = null
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        updateState {
            copy(accounts = DataResource.Data(emptyList()))
        }
    }

    override fun AssetsModelState.reduce() = AssetsViewState(
        balance = accounts.walletBalance(),
        assets = assets.map { assets ->
            assets
                .filter { asset ->
                    // create search term filter predicate
                    asset.shouldBeFiltered(this) &&
                        asset.balance !is DataResource.Loading &&
                        /**
                         * we need to show all fiats
                         */
                        (
                            (asset.balance as? DataResource.Data)?.data?.isPositive == true ||
                                asset.singleAccount is FiatAccount
                            )
                }
                .toHomeAssets()
                .allFiatAndSectionCrypto(sectionSize.size)
        },
        filters = filters,
        showNoResults = assets
            .map { assets ->
                val notFundingOrLoadingAssets = assets.filter {
                    (it.balance as? DataResource.Data)?.data?.let { balance ->
                        balance.isPositive
                    } ?: true
                }
                notFundingOrLoadingAssets.all {
                    it.balance is DataResource.Data && it.shouldBeFiltered(this)
                } || notFundingOrLoadingAssets.isEmpty()
            }.dataOrElse(false),
        showFilterIcon = !assets.allSmallBalances().dataOrElse(true),
        fundsLocks = fundsLocks.map {
            it?.takeIf { it.locks.isNotEmpty() && it.onHoldTotalAmount.isPositive }
        }
    )

    private fun List<HomeAsset>.allFiatAndSectionCrypto(sectionSize: Int): List<HomeAsset> {
        val fiats = filterIsInstance<FiatAssetState>()
        val cryptos = filter { it !in fiats }
        return cryptos.take(sectionSize).plus(fiats)
    }

    private fun List<AssetBalance>.toHomeAssets(): List<HomeAsset> {
        return map { asset ->
            asset.toHomeAsset()
        }.sortedWith(
            compareBy<HomeAsset> {
                (it.fiatBalance as? DataResource.Data)?.data == null
            }.thenComparator { p0: HomeAsset, p1: HomeAsset ->
                val p0Balance = (p0.fiatBalance as? DataResource.Data)?.data
                val p1Balance = (p1.fiatBalance as? DataResource.Data)?.data
                compareValues(p1Balance, p0Balance)
            }
        )
    }

    private fun AssetBalance.toHomeAsset(): HomeAsset {
        return when (val account = singleAccount) {
            is NonCustodialAccount -> NonCustodialAssetState(
                asset = account.currency as AssetInfo,
                icon = listOfNotNull(
                    account.currency.logo,
                    (account.currency as? AssetInfo)?.takeIf { it.isLayer2Token }?.coinNetwork?.nativeAssetTicker
                        ?.let {
                            assetCatalogue.fromNetworkTicker(it)?.logo
                        }
                ),
                name = account.currency.name,
                balance = balance,
                fiatBalance = fiatBalance
            )

            is FiatAccount -> FiatAssetState(
                icon = listOf(account.currency.logo),
                name = account.label,
                balance = balance,
                fiatBalance = fiatBalance,
                account = account
            )

            else -> CustodialAssetState(
                asset = account.currency as AssetInfo,
                icon = listOf(account.currency.logo),
                name = account.currency.name,
                balance = balance,
                fiatBalance = fiatBalance,
                change = exchangeRate24hWithDelta.map { value ->
                    value?.let {
                        ValueChange.fromValue(it.delta24h)
                    } ?: ValueChange.None(0.toDouble())
                }
            )
        }
    }

    override suspend fun handleIntent(modelState: AssetsModelState, intent: AssetsIntent) {
        when (intent) {
            is AssetsIntent.LoadAccounts -> {
                val accounts =
                    if (currencyPrefs.selectedFiatCurrency != modelState.userFiat) {
                        DataResource.Loading
                    } else
                        modelState.accounts
                updateState {
                    copy(
                        sectionSize = intent.sectionSize,
                        accounts = accounts,
                        userFiat = currencyPrefs.selectedFiatCurrency
                    )
                }
                loadAccounts(intent.walletMode)
            }

            is AssetsIntent.LoadFundLocks -> {
                loadFundsLocks(false)
            }

            is AssetsIntent.LoadFilters -> {
                updateState {
                    copy(
                        filters = filterService.filters() + AssetFilter.SearchFilter()
                    )
                }
            }

            is AssetsIntent.FilterSearch -> {
                updateState {
                    copy(
                        filters = filters.minus { it is AssetFilter.SearchFilter }
                            .plus(AssetFilter.SearchFilter(intent.term))
                    )
                }
            }

            is AssetsIntent.UpdateFilters -> {
                filterService.updateFilters(intent.filters)
                updateState {
                    copy(filters = intent.filters)
                }
            }

            AssetsIntent.Refresh -> {
                updateState {
                    copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }
                refreshAccounts()
                if (modelState.walletMode == WalletMode.CUSTODIAL) {
                    loadFundsLocks(true)
                }
            }
        }
    }

    private fun refreshAccounts() {
        viewModelScope.launch {
            walletModeService.walletMode.take(1).flatMapLatest {
                loadAccountsForWalletMode(it, true)
            }.collect()
        }
    }

    private fun loadFundsLocks(forceRefresh: Boolean) {
        fundsLocksJob?.cancel()
        fundsLocksJob = viewModelScope.launch(dispatcher) {
            coincore.getWithdrawalLocks(
                localCurrency = currencyPrefs.selectedFiatCurrency,
                freshnessStrategy = PullToRefresh.freshnessStrategy(
                    shouldGetFresh = forceRefresh,
                    cacheStrategy = RefreshStrategy.RefreshIfStale
                )
            )
                .collectLatest { dataResource ->
                    updateState {
                        copy(fundsLocks = fundsLocks.updateDataWith(dataResource))
                    }
                }
        }
    }

    private fun loadAccounts(walletMode: WalletMode) {
        // take local cache first
        updateState {
            copy(
                walletMode = walletMode,
                accounts = accountsForMode(walletMode)
            )
        }

        accountsJob?.cancel()
        accountsJob = viewModelScope.launch(dispatcher) {
            loadAccountsForWalletMode(walletMode, false).collect()
        }
    }

    private fun loadAccountsForWalletMode(walletMode: WalletMode, forceRefresh: Boolean): Flow<Any> {
        return homeAccountsService.accounts(
            walletMode = walletMode,
            freshnessStrategy = PullToRefresh.freshnessStrategy(shouldGetFresh = forceRefresh)
        )
            .doOnError {
                /**
                 * TODO Handle error for fetching accounts for wallet mode
                 */
                println("Handling exception $it")
            }
            .filterIsInstance<DataResource.Data<List<SingleAccount>>>()
            .distinctUntilChanged { old, new ->
                val oldAssets = old.data.map { it.currency.networkTicker }
                val newAssets = new.data.map { it.currency.networkTicker }
                newAssets.isNotEmpty() && oldAssets.size == newAssets.size && oldAssets.containsAll(newAssets)
            }
            .map { accounts ->
                updateAccountsIfNeeded(
                    accounts.data,
                    modelState.accounts
                )
            }
            .filterIsInstance<DataResource.Data<List<SingleAccount>>>()
            .flatMapLatest { accountsResource ->
                val balances = accountsResource.data.map { account ->
                    account.balance(
                        freshnessStrategy = PullToRefresh.freshnessStrategy(shouldGetFresh = forceRefresh)
                    ).distinctUntilChanged()
                        .map { account to DataResource.Data(it) as DataResource<AccountBalance> }
                        .catch { t ->
                            emit(account to DataResource.Error(t as Exception))
                        }
                }.merge().scan(emptyMap<SingleAccount, DataResource<AccountBalance>>()) { acc, value ->
                    acc + value
                }.onEach { map ->
                    if (map.keys.containsAll(accountsResource.data)) {
                        updateState {
                            copy(
                                accounts = accounts.withBalancedAccounts(
                                    map
                                )
                            )
                        }
                    }
                }

                val majorCurrencyRates = accountsResource.data.map { account ->
                    exchangeRates.exchangeRate(
                        fromAsset = account.currency,
                        toAsset = if (currencyPrefs.selectedFiatCurrency.isMajor())
                            currencyPrefs.selectedFiatCurrency else FiatCurrency.Dollars
                    ).map { it to account }
                }

                val majorRatesFlow = combine(majorCurrencyRates) {
                    it.toList()
                }.filter {
                    it.none { it.first is DataResource.Loading }
                }.onEach {
                    updateState {
                        copy(
                            accounts = accounts.withMajorFiatRates(it)
                        )
                    }
                }

                val exchangeRates = accountsResource.data.map { account ->
                    exchangeRates.getPricesWith24hDelta(
                        fromAsset = account.currency,
                    ).map { it to account }
                }
                val exchangeRatesFlow = combine(exchangeRates) {
                    it.toList()
                }.filter {
                    it.none { it.first is DataResource.Loading }
                }.onEach {
                    updateState {
                        copy(
                            accounts = accounts.withPricings(it)
                        )
                    }
                }
                merge(majorRatesFlow, balances, exchangeRatesFlow)
            }
    }

    /**
     * Check the current model state and the new accounts and updates only if its needed.
     * Returns the updated state accounts
     */
    private fun updateAccountsIfNeeded(
        accounts: List<SingleAccount>,
        stateAccounts: DataResource<List<SingleAccountBalance>>
    ): DataResource<List<SingleAccount>> {
        when (stateAccounts) {
            is DataResource.Loading,
            is DataResource.Error -> {
                updateState {
                    copy(
                        accounts = DataResource.Data(
                            accounts.map { account ->
                                SingleAccountBalance(
                                    singleAccount = account,
                                    balance = DataResource.Loading,
                                    exchangeRate24hWithDelta = DataResource.Loading,
                                    fiatBalance = DataResource.Loading,
                                    majorCurrencyRate = DataResource.Loading
                                )
                            }
                        )
                    )
                }
                return DataResource.Data(accounts)
            }

            is DataResource.Data -> {
                val modelAccounts = stateAccounts.data
                if (modelAccounts.size == accounts.size && modelAccounts.map { it.singleAccount.currency.networkTicker }
                    .containsAll(
                            accounts.map { it.currency.networkTicker }
                        )
                ) {
                    return DataResource.Data(modelAccounts.map { it.singleAccount })
                } else {
                    updateState {
                        copy(
                            accounts = DataResource.Data(
                                accounts.map { account ->
                                    SingleAccountBalance(
                                        singleAccount = account,
                                        balance = DataResource.Loading,
                                        exchangeRate24hWithDelta = DataResource.Loading,
                                        fiatBalance = DataResource.Loading,
                                        majorCurrencyRate = DataResource.Loading
                                    )
                                }
                            )
                        )
                    }
                    return DataResource.Data(accounts)
                }
            }
        }
    }

    private fun DataResource<Iterable<SingleAccountBalance>>.totalBalance(): DataResource<Money> {
        return this.map {
            it.totalAccounts()
        }
    }

    private fun DataResource<Iterable<SingleAccountBalance>>.totalCryptoBalance24hAgo(): DataResource<Money> {
        return this.flatMap { accounts ->
            val cryptoAccounts = accounts.filter { it.singleAccount is CryptoAccount }
            val balances = cryptoAccounts.map { it.balance }
            val exchangeRates = cryptoAccounts.map { it.exchangeRate24hWithDelta }
            when {
                exchangeRates.anyError() -> exchangeRates.getFirstError()
                exchangeRates.anyLoading() -> DataResource.Loading
                balances.any { balance -> balance !is DataResource.Data } ->
                    balances.firstOrNull { it is DataResource.Error }
                        ?: balances.first { it is DataResource.Loading }

                balances.all { balance -> balance is DataResource.Data } -> cryptoAccounts.asSequence().map {
                    when {
                        it.balance is DataResource.Data && it.exchangeRate24hWithDelta is DataResource.Data -> {
                            val exchangeRate24hWithDelta = (it.exchangeRate24hWithDelta as DataResource.Data).data
                            val balance = (it.balance as DataResource.Data).data
                            DataResource.Data(
                                exchangeRate24hWithDelta?.previousRate?.convert(balance)
                            )
                        }

                        it.balance is DataResource.Error -> it.balance
                        it.exchangeRate24hWithDelta is DataResource.Error -> it.exchangeRate24hWithDelta
                        else -> DataResource.Loading
                    }
                }.filterIsInstance<DataResource.Data<Money?>>()
                    .map { it.data }
                    .filter { it?.isPositive == true }
                    .filterNotNull()
                    .fold(Money.zero(modelState.userFiat)) { acc, t ->
                        acc.plus(t)
                    }.let {
                        DataResource.Data(it)
                    }

                else -> throw IllegalStateException("State is not valid ${accounts.map { it.balance }} ")
            }
        }
    }

    private fun Iterable<SingleAccountBalance>.totalAccounts(): Money {
        return map {
            it.fiatBalance
        }.filterIsInstance<DataResource.Data<Money?>>().mapNotNull { it.data }
            .filter { it.isPositive }
            .fold(Money.zero(modelState.userFiat)) { acc, t ->
                acc.plus(t)
            }
    }

    private fun DataResource<Iterable<SingleAccountBalance>>.walletBalance(): WalletBalance {
        val cryptoBalanceNow = filter { it.singleAccount is CryptoAccount }.totalBalance()
        val cryptoBalance24hAgo = totalCryptoBalance24hAgo()

        return WalletBalance(
            balance = totalBalance(),
            cryptoBalanceDifference24h = cryptoBalance24hAgo,
            cryptoBalanceNow = cryptoBalanceNow
        )
    }
}

private fun DataResource<List<AssetBalance>>.allSmallBalances(): DataResource<Boolean> {
    return map { assets ->
        assets.all { it.isSmallBalance() }
    }
}

private fun AssetBalance.shouldBeFiltered(state: AssetsModelState): Boolean {
    val filters = if (state.assets.allSmallBalances().dataOrElse(true)) {
        state.filters.minus { it is AssetFilter.ShowSmallBalances }
    } else {
        state.filters
    }

    return filters.all { it.shouldFilterOut(this) }
}

private fun DataResource<List<SingleAccountBalance>>.withBalancedAccounts(
    balances: Map<SingleAccount, DataResource<AccountBalance>>
): DataResource<List<SingleAccountBalance>> {
    val accounts = (this as? DataResource.Data)?.data ?: return this
    return DataResource.Data(
        balances.mapNotNull { (account, balance) ->
            val oldAccount = accounts.firstOrNull { it.singleAccount == account }
            oldAccount?.let { accountBalance ->
                accountBalance.copy(
                    balance = balance.map { it.total },
                    fiatBalance = balance.map { it.totalFiat }
                )
            }
        }
    )
}

private fun DataResource<List<SingleAccountBalance>>.withMajorFiatRates(
    majorRates: List<Pair<DataResource<ExchangeRate>, SingleAccount>>
): DataResource<List<SingleAccountBalance>> {
    return this.map { accounts ->
        var updatedAccounts = accounts.toList()
        majorRates.forEach { (majorRate, account) ->
            val oldAccount = updatedAccounts.firstOrNull { it.singleAccount == account }
            oldAccount?.let {
                updatedAccounts = updatedAccounts.replace(
                    old = it,
                    new = it.copy(
                        majorCurrencyRate = oldAccount.majorCurrencyRate.updateDataWith(majorRate)
                    )
                )
            }
        }
        updatedAccounts
    }
}

private fun DataResource<List<SingleAccountBalance>>.withPricings(
    prices: List<Pair<DataResource<Prices24HrWithDelta>, SingleAccount>>
): DataResource<List<SingleAccountBalance>> {
    return this.map { accounts ->
        var updatedAccounts = accounts.toList()
        prices.forEach { (price, account) ->
            val oldAccount = updatedAccounts.firstOrNull { it.singleAccount == account }
            oldAccount?.let {
                updatedAccounts = updatedAccounts.replace(
                    old = it,
                    new = it.copy(
                        exchangeRate24hWithDelta = oldAccount.exchangeRate24hWithDelta.updateDataWith(price)
                    )
                )
            }
        }
        updatedAccounts
    }
}

private fun FiatCurrency.isMajor(): Boolean =
    networkTicker in listOf("GBP", "USD", "EUR", "CHF")
