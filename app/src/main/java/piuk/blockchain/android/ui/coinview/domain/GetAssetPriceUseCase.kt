package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.CryptoAsset
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
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

            combineDataResources(historicRates, prices) { historicRatesData, pricesData ->
                val firstPrice = historicRatesData.firstOrNull()?.rate ?: 0.0
                val lastPrice = historicRatesData.lastOrNull()?.rate ?: 0.0
                val difference = lastPrice - firstPrice

                val percentChange = if (timeSpan == HistoricalTimeSpan.DAY) {
                    pricesData.delta24h
                } else {
                    (difference / firstPrice) * 100
                }

                val changeDifference = Money.fromMajor(fiatCurrency, difference.toBigDecimal())

                CoinviewAssetPriceHistory(
                    historicRates = historicRatesData,
                    priceDetail = CoinviewAssetPrice(
                        price = pricesData.currentRate.price,
                        timeSpan = timeSpan,
                        changeDifference = changeDifference,
                        percentChange = percentChange / 100
                    )
                )
            }
        }
    }
}
