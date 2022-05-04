package piuk.blockchain.android.ui.interest.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.AssetInfo

sealed interface InterestDashboardIntents : Intent<InterestDashboardModelState> {
    object LoadDashboard : InterestDashboardIntents

    data class FilterData(val filter: String) : InterestDashboardIntents

    data class InterestItemClicked(
        val cryptoCurrency: AssetInfo,
        val hasBalance: Boolean
    ) : InterestDashboardIntents

    object StartKyc : InterestDashboardIntents
}
