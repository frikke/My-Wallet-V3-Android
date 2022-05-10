package com.blockchain.api.services

import androidx.annotation.VisibleForTesting
import com.blockchain.api.assetprice.AssetPriceApiInterface
import com.blockchain.api.assetprice.data.AssetPriceDto
import com.blockchain.api.assetprice.data.PriceRequestPairDto
import com.blockchain.api.assetprice.data.PriceSymbolDto
import io.reactivex.rxjava3.core.Single

enum class PriceTimescale(val intervalSeconds: Int) {
    FIFTEEN_MINUTES(900),
    ONE_HOUR(3600),
    TWO_HOURS(7200),
    ONE_DAY(86400),
    FIVE_DAYS(432000)
}

data class AssetSymbol(
    val ticker: String,
    val name: String,
    val precisionDp: Int,
    val isFiat: Boolean
)

data class SupportedAssetSymbols(
    val base: List<AssetSymbol>,
    val quote: List<AssetSymbol>
)

data class AssetPrice(
    val base: String,
    val quote: String,
    val price: Double,
    val timestampSeconds: Long
)

class AssetPriceService internal constructor(
    private val api: AssetPriceApiInterface,
    private val apiCode: String
) {
    /** All the symbols supported by the price API */
    fun getSupportedCurrencies(): Single<SupportedAssetSymbols> =
        api.getAvailableSymbols(apiCode)
            .map { dto ->
                SupportedAssetSymbols(
                    base = dto.baseSymbols.values.map { it.toAssetSymbol() },
                    quote = dto.quoteSymbols.values.map { it.toAssetSymbol() }
                )
            }

    /** Get the current prices of the requested assets in all the requested targets */
    fun getCurrentPrices(
        baseTickerList: Set<String>,
        quoteTickerList: Set<String>
    ): Single<List<AssetPrice>> =
        api.getCurrentPrices(
            pairs = expandToPairs(
                baseTickerList,
                quoteTickerList
            ),
            apiKey = apiCode
        ).map { result ->
            result.map {
                it.value.toAssetPrice(it.key) ?: unavailablePrice(it.key)
            }
        }

    /** Get the historical prices of the requested assets in all the requested targets */
    fun getHistoricPrices(
        baseTickers: Set<String>,
        quoteTickers: Set<String>,
        time: Long // Seconds before now
    ): Single<List<AssetPrice>> =
        api.getHistoricPrices(
            pairs = expandToPairs(baseTickers, quoteTickers),
            time = time,
            apiKey = apiCode
        ).map { result ->
            result.map {
                it.value.toAssetPrice(it.key) ?: unavailablePrice(it.key)
            }
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun expandToPairs(
        sourceTickerList: Set<String>,
        targetTickerList: Set<String>
    ): List<PriceRequestPairDto> =
        sourceTickerList
            .map { src ->
                val targets = targetTickerList.filterNot { src == it }
                targets.map { tgt ->
                    PriceRequestPairDto(
                        base = src,
                        quote = tgt
                    )
                }
            }.flatten()

    /** Get a series of historical price index which covers a range of time represents in specific scale */
    fun getHistoricPriceSeriesSince(
        base: String,
        quote: String,
        start: Long, // Epoch seconds
        scale: PriceTimescale
    ): Single<List<AssetPrice>> =
        api.getHistoricPriceSince(
            base = base,
            quote = quote,
            start = start,
            scale = scale.intervalSeconds,
            apiKey = apiCode
        ).map { list ->
            list.filterNot { it.price == null }.map { it.toAssetPrice(base, quote) }
        }

    private fun unavailablePrice(pair: String): AssetPrice =
        AssetPrice(
            base = pair.extractBase(),
            quote = pair.extractQuote(),
            price = Double.NaN,
            timestampSeconds = System.currentTimeMillis() / 1000
        )
}

private fun PriceSymbolDto.toAssetSymbol(): AssetSymbol =
    AssetSymbol(
        ticker = ticker,
        name = name,
        precisionDp = precisionDp,
        isFiat = isFiat
    )

private fun AssetPriceDto.toAssetPrice(base: String, quote: String): AssetPrice =
    AssetPrice(
        base = base,
        quote = quote,
        price = price ?: Double.NaN,
        timestampSeconds = timestampSeconds
    )

private fun AssetPriceDto.toAssetPrice(pair: String): AssetPrice =
    AssetPrice(
        base = pair.extractBase(),
        quote = pair.extractQuote(),
        price = price ?: Double.NaN,
        timestampSeconds = timestampSeconds
    )

private fun String.extractBase(): String = substringBeforeLast("-")
private fun String.extractQuote(): String = substringAfterLast("-")
