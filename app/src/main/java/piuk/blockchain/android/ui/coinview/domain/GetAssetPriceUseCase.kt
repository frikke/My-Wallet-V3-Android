package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.CryptoAsset
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory

object GetAssetPriceUseCase {
    operator fun invoke(
        asset: CryptoAsset,
        timeSpan: HistoricalTimeSpan,
        fiat: FiatCurrency
    ): Flow<DataResource<CoinviewAssetPriceHistory>> {
        return combine(
            asset.historicRateSeries(timeSpan),
            asset.getPricesWith24hDelta()
        ) { historicRates, prices ->
            val results = listOf(historicRates, prices)

            when {
                results.any { it is DataResource.Loading } -> {
                    DataResource.Loading
                }

                results.any { it is DataResource.Error } -> {
                    DataResource.Error((results.first { it is DataResource.Error } as DataResource.Error).error)
                }

                else -> {
                    historicRates as DataResource.Data
                    prices as DataResource.Data

                    val firstPrice = historicRates.data.firstOrNull()?.rate ?: 0.0
                    val lastPrice = historicRates.data.lastOrNull()?.rate ?: 0.0
                    val difference = lastPrice - firstPrice

                    val percentChange =
                        if (timeSpan == HistoricalTimeSpan.DAY) prices.data.delta24h
                        else (difference / firstPrice) * 100

                    val changeDifference = Money.fromMajor(fiat, difference.toBigDecimal())

                    DataResource.Data(
                        CoinviewAssetPriceHistory(
                            historicRates = historicRates.data,
                            priceDetail = CoinviewAssetPrice(
                                price = prices.data.currentRate.price,
                                timeSpan = timeSpan,
                                changeDifference = changeDifference,
                                percentChange = percentChange / 100
                            )
                        )
                    )
                }
            }
        }
    }
}