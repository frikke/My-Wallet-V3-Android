package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import info.blockchain.balance.Money

data class CoinViewAssetPrice(
    val historicRates: List<HistoricalRate>,
    val timeSpan: HistoricalTimeSpan,
    val price: Money,
    val changeDifference: Money,
    val percentChange: Double
)