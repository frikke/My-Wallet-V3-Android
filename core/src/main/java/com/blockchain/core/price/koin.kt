package com.blockchain.core.price

import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.core.price.historic.HistoricRateLocalSource
import com.blockchain.core.price.historic.HistoricRateRemoteSource
import com.blockchain.core.price.impl.ExchangeRatesDataManagerImpl
import com.blockchain.core.price.impl.assetpricestore.AssetPriceStore
import com.blockchain.core.price.impl.assetpricestore.AssetPriceStoreCache
import com.blockchain.core.price.impl.assetpricestore.SupportedTickersStore
import org.koin.dsl.bind
import org.koin.dsl.module

val pricesModule = module {

    single {
        ExchangeRatesDataManagerImpl(
            priceStore = get(),
            assetPriceService = get(),
            currencyPrefs = get(),
            assetCatalogue = get()
        )
    }.bind(ExchangeRatesDataManager::class)
        .bind(ExchangeRates::class)

    factory {
        AssetPriceStore(
            cache = get(),
            supportedTickersStore = get()
        )
    }

    factory {
        AssetPriceStoreCache(
            assetPriceService = get(),
            supportedTickersStore = get()
        )
    }

    factory {
        SupportedTickersStore(
            assetPriceService = get()
        )
    }

    factory {
        HistoricRateLocalSource(database = get())
    }

    factory {
        HistoricRateRemoteSource(exchangeRates = get())
    }

    single {
        HistoricRateFetcher(
            localSource = get(),
            remoteSource = get()
        )
    }
}
