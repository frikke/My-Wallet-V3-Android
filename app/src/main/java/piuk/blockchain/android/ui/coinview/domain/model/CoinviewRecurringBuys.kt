package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.core.recurringbuy.domain.model.RecurringBuy

data class CoinviewRecurringBuys(
    val data: List<RecurringBuy>,
    val isAvailableForTrading: Boolean
)
