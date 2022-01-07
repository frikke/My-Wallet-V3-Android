package com.blockchain.core.price

import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.core.price.historic.HistoricRateLocalSource
import com.blockchain.core.price.historic.HistoricRateRemoteSource
import com.blockchain.core.price.impl.AssetPriceStore
import com.blockchain.core.price.impl.ExchangeRatesDataManagerImpl
import com.blockchain.core.price.impl.SparklineCallCache
import org.koin.dsl.bind
import org.koin.dsl.module

val pricesModule = module {

    factory {
        SparklineCallCache(
            priceService = get()
        )
    }

    single {
        ExchangeRatesDataManagerImpl(
            priceStore = get(),
            sparklineCall = get(),
            assetPriceService = get(),
            currencyPrefs = get()
        )
    }.bind(ExchangeRatesDataManager::class)
        .bind(ExchangeRates::class)

    factory {
        AssetPriceStore(
            assetPriceService = get(),
            assetCatalogue = get(),
            prefs = get()
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
