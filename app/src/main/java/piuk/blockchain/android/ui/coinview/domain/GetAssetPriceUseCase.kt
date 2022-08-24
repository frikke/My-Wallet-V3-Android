package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.CryptoAsset
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import com.blockchain.data.anyError
import com.blockchain.data.anyLoading
import com.blockchain.data.getFirstError
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
        fiatCurrency: FiatCurrency
    ): Flow<DataResource<CoinviewAssetPriceHistory>> {
        return combine(
            asset.historicRateSeries(timeSpan),
            asset.getPricesWith24hDelta()
        ) { historicRates, prices ->
            val results = listOf(historicRates, prices)

            when {
                results.anyLoading() -> {
                    DataResource.Loading
                }

                results.anyError() -> {
                    DataResource.Error(results.getFirstError().error)
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

                    val changeDifference = Money.fromMajor(fiatCurrency, difference.toBigDecimal())

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
