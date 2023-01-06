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
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.domain.FiltersService
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.home.domain.ModelAccount
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

class AssetsViewModel(
    private val homeAccountsService: HomeAccountsService,
    private val currencyPrefs: CurrencyPrefs,
    private val assetCatalogue: AssetCatalogue,
    private val exchangeRates: ExchangeRatesDataManager,
    private val walletModeService: WalletModeService,
    private val filterService: FiltersService,
    private val coincore: Coincore
) : MviViewModel<AssetsIntent, AssetsViewState, AssetsModelState, HomeNavEvent, ModelConfigArgs.NoArgs>(
    AssetsModelState(walletMode = WalletMode.CUSTODIAL_ONLY, userFiat = currencyPrefs.selectedFiatCurrency)
) {
    private var accountsJob: Job? = null
    private var fundsLocksJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        updateState { state ->
            state.copy(accounts = DataResource.Data(emptyList()))
        }
    }

    override fun reduce(state: AssetsModelState): AssetsViewState {
        return with(state) {
            AssetsViewState(
                balance = accounts.walletBalance(),
                assets = state.accounts.map { modelAccounts ->
                    modelAccounts
                        .filter { modelAccount ->
                            // create search term filter predicate
                            modelAccount.shouldBeFiltered(state) &&
                                modelAccount.balance !is DataResource.Loading &&
                                (modelAccount.balance as? DataResource.Data)?.data?.isPositive == true
                        }
                        .toHomeAssets()
                        .allFiatAndSectionCrypto(state.sectionSize.size)
                },

                filters = filters,
                showNoResults = state.accounts.map { modelAccounts ->
                    modelAccounts.none { it.shouldBeFiltered(state) } && modelAccounts.isNotEmpty()
                }.dataOrElse(false),
                fundsLocks = state.fundsLocks.map { it?.takeIf { it.locks.isNotEmpty() } }
            )
        }
    }

    private fun List<HomeAsset>.allFiatAndSectionCrypto(sectionSize: Int): List<HomeAsset> {
        val fiats = filterIsInstance<FiatAssetState>()
        val cryptos = filter { it !in fiats }
        return cryptos.take(sectionSize).plus(fiats)
    }

    private fun List<ModelAccount>.toHomeAssets(): List<HomeAsset> {
        val grouped = sortedWith(
            compareByDescending<ModelAccount> { it.singleAccount.currency.index }
                .thenBy {
                    it.singleAccount.currency.name
                }
        )
            .groupBy(
                keySelector = {
                    it.singleAccount.currency.networkTicker
                }
            )

        return grouped.values.map { accounts ->
            accounts.toHomeAsset()
        }.sortedWith(
            object : Comparator<HomeAsset> {
                override fun compare(p0: HomeAsset, p1: HomeAsset): Int {
                    val p0Balance = (p0.fiatBalance as? DataResource.Data) ?: return 0
                    val p1Balance = (p1.fiatBalance as? DataResource.Data) ?: return 0
                    return p1Balance.data.compareTo(p0Balance.data)
                }
            }
        )
    }

    private fun List<ModelAccount>.toHomeAsset(): HomeAsset {
        require(this.map { it.singleAccount.currency.networkTicker }.distinct().size == 1)
        return when (val first = first().singleAccount) {
            is NonCustodialAccount -> NonCustodialAssetState(
                asset = first.currency as AssetInfo,
                icon = listOfNotNull(
                    first.currency.logo,
                    (first.currency as? AssetInfo)?.l1chainTicker?.let { l1 ->
                        assetCatalogue.fromNetworkTicker(l1)?.logo
                    }
                ),
                name = first.currency.name,
                balance = map { acc -> acc.balance }.sumAvailableBalances(),
                fiatBalance = map { acc -> acc.fiatBalance }.sumAvailableBalances(),
            )
            is FiatAccount -> FiatAssetState(
                icon = listOf(first.currency.logo),
                name = first.label,
                balance = map { acc -> acc.balance }.sumAvailableBalances(),
                fiatBalance = map { acc -> acc.fiatBalance }.sumAvailableBalances(),
                account = first
            )
            else -> CustodialAssetState(
                asset = first.currency as AssetInfo,
                icon = listOf(first.currency.logo),
                name = first.currency.name,
                balance = map { acc -> acc.balance }.sumAvailableBalances(),
                fiatBalance = map { acc -> acc.fiatBalance }.sumAvailableBalances(),
                change = this.first().exchangeRate24hWithDelta.map { value ->
                    ValueChange.fromValue(value.delta24h)
                }
            )
        }
    }

    override suspend fun handleIntent(modelState: AssetsModelState, intent: AssetsIntent) {
        when (intent) {
            is AssetsIntent.LoadAccounts -> {
                val accounts =
                    if (currencyPrefs.selectedFiatCurrency != modelState.userFiat)
                        DataResource.Loading
                    else
                        modelState.accounts
                updateState {
                    it.copy(
                        sectionSize = intent.sectionSize,
                        accounts = accounts,
                        userFiat = currencyPrefs.selectedFiatCurrency
                    )
                }
                loadAccounts()
            }

            is AssetsIntent.LoadFundLocks -> {
                loadFundsLocks()
            }

            is AssetsIntent.LoadFilters -> {
                updateState {
                    it.copy(
                        filters = filterService.filters() + AssetFilter.SearchFilter()
                    )
                }
            }

            is AssetsIntent.FilterSearch -> {
                updateState {
                    it.copy(
                        filters = it.filters.minus { it is AssetFilter.SearchFilter }
                            .plus(AssetFilter.SearchFilter(intent.term))
                    )
                }
            }

            is AssetsIntent.UpdateFilters -> {
                filterService.updateFilters(intent.filters)
                updateState {
                    it.copy(filters = intent.filters)
                }
            }
        }
    }

    private fun loadFundsLocks() {
        fundsLocksJob?.cancel()
        fundsLocksJob = viewModelScope.launch {
            coincore.getWithdrawalLocks(localCurrency = currencyPrefs.selectedFiatCurrency)
                .collectLatest { dataResource ->
                    updateState {
                        it.copy(fundsLocks = it.fundsLocks.updateDataWith(dataResource))
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadAccounts() {
        accountsJob?.cancel()
        accountsJob = viewModelScope.launch {
            walletModeService.walletMode.onEach { walletMode ->
                if (walletMode != modelState.walletMode) {
                    updateState {
                        it.copy(
                            walletMode = walletMode,
                            accounts = it.accountsForMode(walletMode)
                        )
                    }
                }
            }.flatMapLatest {
                homeAccountsService.accounts(it)
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
                    .flatMapLatest { accounts ->
                        val balances = accounts.data.map { account ->
                            account.balance().distinctUntilChanged()
                                .map { account to DataResource.Data(it) as DataResource<AccountBalance> }
                                .catch { t ->
                                    emit(account to DataResource.Error(t as Exception))
                                }
                        }.merge().scan(emptyMap<SingleAccount, DataResource<AccountBalance>>()) { acc, value ->
                            acc + value
                        }.onEach { map ->
                            if (map.keys.containsAll(accounts.data)) {
                                updateState { state ->
                                    state.copy(
                                        accounts = state.accounts.withBalancedAccounts(
                                            map
                                        )
                                    )
                                }
                            }
                        }

                        val usdRate = accounts.data.map { account ->
                            exchangeRates.exchangeRate(fromAsset = account.currency, toAsset = FiatCurrency.Dollars)
                                .map { it to account }
                        }.merge().onEach { (usdExchangeRate, account) ->
                            updateState { state ->
                                state.copy(
                                    accounts = state.accounts.withUsdRate(
                                        account = account,
                                        usdRate = usdExchangeRate
                                    )
                                )
                            }
                        }

                        val exchangeRates = accounts.data.map { account ->
                            exchangeRates.getPricesWith24hDelta(fromAsset = account.currency)
                                .map { it to account }
                        }.merge().onEach { (price, account) ->
                            updateState { state ->
                                state.copy(
                                    accounts = state.accounts.withPricing(account, price)
                                )
                            }
                        }
                        merge(usdRate, balances, exchangeRates)
                    }
            }.collect()
        }
    }

    /**
     * Check the current model state and the new accounts and updates only if its needed.
     * Returns the updated state accounts
     */
    private fun updateAccountsIfNeeded(
        accounts: List<SingleAccount>,
        stateAccounts: DataResource<List<ModelAccount>>
    ): DataResource<List<SingleAccount>> {
        when (stateAccounts) {
            is DataResource.Loading,
            is DataResource.Error -> {
                updateState {
                    it.copy(
                        accounts = DataResource.Data(
                            accounts.map { account ->
                                ModelAccount(
                                    singleAccount = account,
                                    balance = DataResource.Loading,
                                    exchangeRate24hWithDelta = DataResource.Loading,
                                    fiatBalance = DataResource.Loading,
                                    usdRate = DataResource.Loading
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
                        it.copy(
                            accounts = DataResource.Data(
                                accounts.map { account ->
                                    ModelAccount(
                                        singleAccount = account,
                                        balance = DataResource.Loading,
                                        exchangeRate24hWithDelta = DataResource.Loading,
                                        fiatBalance = DataResource.Loading,
                                        usdRate = DataResource.Loading
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

    private fun DataResource<Iterable<ModelAccount>>.totalBalance(): DataResource<Money> {
        return this.map {
            it.totalAccounts()
        }
    }

    private fun DataResource<Iterable<ModelAccount>>.totalCryptoBalance24hAgo(): DataResource<Money> {
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
                balances.all { balance -> balance is DataResource.Data } -> cryptoAccounts.map {
                    when {
                        it.balance is DataResource.Data && it.exchangeRate24hWithDelta is DataResource.Data -> {

                            val exchangeRate24hWithDelta = (it.exchangeRate24hWithDelta as DataResource.Data).data
                            val balance = (it.balance as DataResource.Data).data
                            DataResource.Data(
                                exchangeRate24hWithDelta.previousRate.convert(balance)
                            )
                        }
                        it.balance is DataResource.Error -> it.balance
                        it.exchangeRate24hWithDelta is DataResource.Error -> it.exchangeRate24hWithDelta
                        else -> DataResource.Loading
                    }
                }.filterIsInstance<DataResource.Data<Money>>()
                    .map { it.data }
                    .fold(Money.zero(modelState.userFiat)) { acc, t ->
                        acc.plus(t)
                    }.let {
                        DataResource.Data(it)
                    }
                else -> throw IllegalStateException("State is not valid ${accounts.map { it.balance }} ")
            }
        }
    }

    private fun Iterable<ModelAccount>.totalAccounts(): Money {
        return map {
            it.fiatBalance
        }.filterIsInstance<DataResource.Data<Money>>()
            .map { it.data }
            .fold(Money.zero(modelState.userFiat)) { acc, t ->
                acc.plus(t)
            }
    }

    private fun DataResource<Iterable<ModelAccount>>.walletBalance(): WalletBalance {
        val cryptoBalanceNow = filter { it.singleAccount is CryptoAccount }.totalBalance()
        val cryptoBalance24hAgo = totalCryptoBalance24hAgo()

        return WalletBalance(
            balance = totalBalance(),
            cryptoBalanceDifference24h = cryptoBalance24hAgo,
            cryptoBalanceNow = cryptoBalanceNow
        )
    }
}

private fun ModelAccount.shouldBeFiltered(state: AssetsModelState): Boolean {
    return state.filters.all { it.shouldFilterOut(this) }
}

private fun List<DataResource<Money>>.sumAvailableBalances(): DataResource<Money> {
    var total: DataResource<Money>? = null
    forEach { money ->
        total = when (total) {
            is DataResource.Loading,
            is DataResource.Error,
            null -> money
            is DataResource.Data -> DataResource.Data(
                (total as DataResource.Data<Money>).data.plus(
                    (money as? DataResource.Data)?.data
                        ?: Money.zero((total as DataResource.Data<Money>).data.currency)
                )
            )
        }
    }
    return total!!
}

private fun DataResource<List<ModelAccount>>.withBalancedAccounts(
    balances: Map<SingleAccount, DataResource<AccountBalance>>
): DataResource<List<ModelAccount>> {
    val accounts = (this as? DataResource.Data)?.data ?: return this
    return DataResource.Data(
        balances.map { (account, balance) ->
            val oldAccount = accounts.first { it.singleAccount == account }
            oldAccount.copy(
                balance = balance.map { it.total },
                fiatBalance = balance.map { it.totalFiat }
            )
        }
    )
}

private fun DataResource<List<ModelAccount>>.withUsdRate(
    account: SingleAccount,
    usdRate: DataResource<ExchangeRate>
): DataResource<List<ModelAccount>> {
    return this.map { accounts ->
        val oldAccount = accounts.first { it.singleAccount == account }
        accounts.replace(
            old = oldAccount,
            new = oldAccount.copy(
                usdRate = oldAccount.usdRate.updateDataWith(usdRate)
            )
        )
    }
}

private fun DataResource<List<ModelAccount>>.withPricing(
    account: SingleAccount,
    price: DataResource<Prices24HrWithDelta>
): DataResource<List<ModelAccount>> {
    return this.map { accounts ->
        val oldAccount = accounts.first { it.singleAccount == account }
        accounts.replace(
            old = oldAccount,
            new = oldAccount.copy(
                exchangeRate24hWithDelta = oldAccount.exchangeRate24hWithDelta.updateDataWith(
                    price
                )
            )
        )
    }
}
