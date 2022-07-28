package com.blockchain.core.fiatcurrencies

import com.blockchain.analytics.Analytics
import com.blockchain.api.services.FiatCurrenciesApiService
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.fiatcurrencies.model.TradingCurrencies
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.firstOutcome
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

internal class FiatCurrenciesRepository(
    private val authenticator: Authenticator,
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

    override suspend fun getTradingCurrencies(fresh: Boolean): Outcome<Exception, TradingCurrencies> =
        getUserStore.stream(FreshnessStrategy.Cached(forceRefresh = fresh)).firstOutcome()
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

    override fun getTradingCurrenciesFlow(): Flow<TradingCurrencies> {
        return userService.getUserFlow()
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
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { authHeader ->
                api.setSelectedTradingCurrency(authHeader, currency.networkTicker)
                    .mapError { it.exception }
                    .doOnSuccess {
                        @Suppress("DEPRECATION_ERROR")
                        currencyPrefs.tradingCurrency = currency
                        analytics.logEvent(CurrencySelectionAnalytics.TradingCurrencyChanged(currency))
                    }
            }.flatMap {
                // Fetching updated trading currencies and ignoring its result
                getUserStore.markAsStale()
                getTradingCurrencies().map { Unit }
            }
}
