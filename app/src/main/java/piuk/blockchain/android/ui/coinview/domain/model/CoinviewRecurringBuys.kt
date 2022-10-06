package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.nabu.models.data.RecurringBuy

data class CoinviewRecurringBuys(
    val data: List<RecurringBuy>,
    val isAvailableForTrading: Boolean
)
