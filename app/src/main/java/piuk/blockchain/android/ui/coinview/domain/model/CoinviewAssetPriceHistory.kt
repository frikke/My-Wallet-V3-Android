package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import info.blockchain.balance.Money

data class CoinviewAssetPriceHistory(
    val historicRates: List<HistoricalRate>,
    val priceDetail: CoinviewAssetPrice
)

data class CoinviewAssetPrice(
    val price: Money,
    val timeSpan: HistoricalTimeSpan,
    val changeDifference: Money,
    val percentChange: Double
)
