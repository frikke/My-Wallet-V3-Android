package com.blockchain.home.presentation.allassets

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.extensions.replace
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.home.model.AssetFilter
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class AssetsViewModel(
    private val homeAccountsService: HomeAccountsService,
    private val currencyPrefs: CurrencyPrefs,
    private val exchangeRates: ExchangeRatesDataManager,
) : MviViewModel<AssetsIntent, AssetsViewState, AssetsModelState, HomeNavEvent, ModelConfigArgs.NoArgs>(
    AssetsModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        updateState { state ->
            state.copy(accounts = DataResource.Data(emptyList()))
        }
    }

    override fun reduce(state: AssetsModelState): AssetsViewState {
        return with(state) {
            AssetsViewState(
                balance = accounts.totalBalance(),
                cryptoAssets = state.accounts.map { modelAccounts ->
                    modelAccounts
                        .filter { modelAccount ->
                            // create search term filter predicate
                            val searchTermPredicate = if (state.filterTerm.isEmpty()) {
                                true
                            } else {
                                with(modelAccount.singleAccount.currency) {
                                    displayTicker.contains(state.filterTerm, ignoreCase = true) ||
                                        name.contains(state.filterTerm, ignoreCase = true)
                                }
                            }

                            // create predicate for all filters
                            val filtersPredicate = filters.map { assetFilter ->
                                when (assetFilter.filter) {
                                    AssetFilter.ShowSmallBalances -> {
                                        if (assetFilter.isEnabled) {
                                            // auto pass check
                                            true
                                        } else {
                                            // filter out small balances
                                            (modelAccount.fiatBalance.map {
                                                it >= Money.fromMajor(it.currency, AssetFilter.MinimumBalance)
                                            } as? DataResource.Data)?.data.let { isHighBalance ->
                                                isHighBalance != false
                                            }
                                        }
                                    }
                                }

                            }.all { it /*if all filters are true*/ }

                            // filter accounts matching CryptoAccount and the search predicate
                            (modelAccount.singleAccount is CryptoAccount) && searchTermPredicate && filtersPredicate
                        }
                        .toHomeCryptoAssets()
                        .let { accounts ->
                            // <display list / isFullList>
                            accounts.take(state.sectionSize.size) to (accounts.size > state.sectionSize.size)
                        }
                },
                fiatAssets = DataResource.Data(emptyList()),
                filters = filters
            )
        }
    }

    private fun List<ModelAccount>.toHomeCryptoAssets(): List<CryptoAssetState> {
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

        return grouped.values.map {
            CryptoAssetState(
                icon = it.first().singleAccount.currency.logo,
                name = it.first().singleAccount.currency.name,
                balance = it.map { acc -> acc.balance }.sumAvailableBalances(),
                fiatBalance = it.map { acc -> acc.fiatBalance }.sumAvailableBalances(),
                change = it.first().exchangeRateDayDelta.map { value ->
                    ValueChange.fromValue(value)
                }
            )
        }.sortedWith(
            object : Comparator<CryptoAssetState> {
                override fun compare(p0: CryptoAssetState, p1: CryptoAssetState): Int {
                    val p0Balance = (p0.fiatBalance as? DataResource.Data) ?: return 0
                    val p1Balance = (p1.fiatBalance as? DataResource.Data) ?: return 0
                    return p1Balance.data.compareTo(p0Balance.data)
                }
            }
        )
    }

    override suspend fun handleIntent(modelState: AssetsModelState, intent: AssetsIntent) {
        when (intent) {
            is AssetsIntent.LoadHomeAccounts -> {
                updateState { it.copy(sectionSize = intent.sectionSize) }
                loadAccounts()
                loadFilters()
            }

            is AssetsIntent.FilterSearch -> {
                updateState {
                    it.copy(filterTerm = intent.term)
                }
            }

            is AssetsIntent.UpdateFilters -> {
                homeAccountsService.updateFilters(intent.filters)
            }
        }
    }

    private fun loadFilters() {
        viewModelScope.launch {
            homeAccountsService.filters()
                .onEach { assetFilters ->
                    updateState {
                        it.copy(filters = assetFilters)
                    }
                }
                .collect()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadAccounts() {
        viewModelScope.launch {
            homeAccountsService.accounts()
                .onStart {
                    updateState { state ->
                        state.copy(
                            accounts = DataResource.Loading
                        )
                    }
                }
                .filterIsInstance<DataResource.Data<List<SingleAccount>>>()
                .distinctUntilChanged { old, new ->
                    val oldAssets = old.data.map { it.currency.networkTicker }
                    val newAssets = new.data.map { it.currency.networkTicker }
                    newAssets.isNotEmpty() && oldAssets.size == newAssets.size && oldAssets.containsAll(newAssets)
                }
                .onEach { data ->
                    updateState { state ->
                        state.copy(
                            accounts = DataResource.Data(
                                data.data.map { account ->
                                    ModelAccount(
                                        singleAccount = account,
                                        balance = DataResource.Loading,
                                        exchangeRateDayDelta = DataResource.Loading,
                                        fiatBalance = DataResource.Loading
                                    )
                                }
                            )
                        )
                    }
                }
                .filterIsInstance<DataResource.Data<List<SingleAccount>>>()
                .flatMapLatest { accounts ->
                    val balances = accounts.data.map { account ->
                        account.balance.distinctUntilChanged().map { balance ->
                            account to balance
                        }
                    }.merge().onEach { (account, balance) ->
                        updateState { state ->
                            state.copy(
                                accounts = state.accounts.withBalancedAccount(account, balance)
                            )
                        }
                    }

                    val exchangeRates = accounts.data.map { account ->
                        exchangeRates.getPricesWith24hDelta(
                            fromAsset = account.currency
                        ).map {
                            it to account
                        }
                    }.merge().onEach { (price, account) ->
                        updateState { state ->
                            state.copy(
                                accounts = state.accounts.withPricing(account, price)
                            )
                        }
                    }
                    merge(exchangeRates, balances)
                }.collect()
        }
    }

    private fun DataResource<List<ModelAccount>>.totalBalance(): DataResource<Money> {
        return this.map {
            it.totalAccounts()
        }
    }

    private fun List<ModelAccount>.totalAccounts(): Money {
        return map { it.fiatBalance }.filterIsInstance<DataResource.Data<Money>>()
            .map { it.data }
            .fold(Money.zero(currencyPrefs.selectedFiatCurrency)) { acc, t ->
                acc.plus(t)
            }
    }
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

private fun DataResource<List<ModelAccount>>.withBalancedAccount(
    account: SingleAccount,
    balance: AccountBalance
): DataResource<List<ModelAccount>> {
    return this.map { accounts ->
        val oldAccount = accounts.first { it.singleAccount == account }
        accounts.replace(
            old = oldAccount,
            new = oldAccount.copy(
                balance = DataResource.Data(balance.total),
                fiatBalance = DataResource.Data(balance.totalFiat)
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
                exchangeRateDayDelta = oldAccount.exchangeRateDayDelta.updateDataWith(
                    price.map {
                        it.delta24h
                    }
                )
            )
        )
    }
}


