package com.blockchain.core.fiatcurrencies

import com.blockchain.analytics.Analytics
import com.blockchain.api.services.FiatCurrenciesApiService
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.firstOutcome
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.fiatcurrencies.model.TradingCurrencies
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class FiatCurrenciesRepository(
    private val getUserStore: GetUserStore,
    private val userService: UserService,
    private val assetCatalogue: AssetCatalogue,
    private val currencyPrefs: CurrencyPrefs,
    private val analytics: Analytics,
    private val api: FiatCurrenciesApiService
) : FiatCurrenciesService {

    @Suppress("DEPRECATION_ERROR")
    override val selectedTradingCurrency: FiatCurrency
        get() = currencyPrefs.tradingCurrency
            ?: throw UninitializedPropertyAccessException("Should have been initialized at app startup")

    override suspend fun getTradingCurrencies(fresh: Boolean): Outcome<Exception, TradingCurrencies> {
        val refreshStrategy = if (fresh) RefreshStrategy.ForceRefresh else RefreshStrategy.RefreshIfStale
        return getUserStore.stream(FreshnessStrategy.Cached(refreshStrategy)).firstOutcome()
            .map { user ->
                TradingCurrencies(
                    selected = assetCatalogue.fiatFromNetworkTicker(user.currencies.preferredFiatTradingCurrency)
                        ?: FiatCurrency.Dollars,
                    allRecommended = user.currencies.userFiatCurrencies.mapNotNull {
                        assetCatalogue.fiatFromNetworkTicker(it)
                    },
                    allAvailable = user.currencies.usableFiatCurrencies.mapNotNull {
                        assetCatalogue.fiatFromNetworkTicker(it)
                    }
                ).also {
                    @Suppress("DEPRECATION_ERROR")
                    currencyPrefs.tradingCurrency = it.selected
                }
            }
    }

    override fun getTradingCurrenciesFlow(freshnessStrategy: FreshnessStrategy): Flow<TradingCurrencies> {
        return userService.getUserFlow(freshnessStrategy)
            .map { user ->
                TradingCurrencies(
                    selected = assetCatalogue.fiatFromNetworkTicker(user.currencies.preferredFiatTradingCurrency)
                        ?: FiatCurrency.Dollars,
                    allRecommended = user.currencies.userFiatCurrencies.mapNotNull {
                        assetCatalogue.fiatFromNetworkTicker(it)
                    },
                    allAvailable = user.currencies.usableFiatCurrencies.mapNotNull {
                        assetCatalogue.fiatFromNetworkTicker(it)
                    }
                ).also {
                    @Suppress("DEPRECATION_ERROR")
                    currencyPrefs.tradingCurrency = it.selected
                }
            }
    }

    override suspend fun setSelectedTradingCurrency(currency: FiatCurrency): Outcome<Exception, Unit> =
        api.setSelectedTradingCurrency(currency.networkTicker)
            .mapError { it }
            .doOnSuccess {
                @Suppress("DEPRECATION_ERROR")
                currencyPrefs.tradingCurrency = currency
                analytics.logEvent(CurrencySelectionAnalytics.TradingCurrencyChanged(currency))
            }.flatMap {
                // Fetching updated trading currencies and ignoring its result
                getUserStore.markAsStale()
                getTradingCurrencies().map { Unit }
            }
}
