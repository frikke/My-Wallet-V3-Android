package piuk.blockchain.android.ui.interest.tbm.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.AssetInfo

sealed interface InterestDashboardIntents : Intent<InterestDashboardModelState> {
    object LoadData : InterestDashboardIntents

    data class FilterData(val filter: String) : InterestDashboardIntents

    data class InterestItemClicked(
        val cryptoCurrency: AssetInfo,
        val hasBalance: Boolean
    ) : InterestDashboardIntents
}