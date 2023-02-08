package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.domain.trade.model.RecurringBuy

data class CoinviewRecurringBuys(
    val data: List<RecurringBuy>,
    val isAvailableForTrading: Boolean
)
