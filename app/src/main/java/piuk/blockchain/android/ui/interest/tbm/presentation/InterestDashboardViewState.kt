package piuk.blockchain.android.ui.interest.tbm.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class InterestDashboardViewState(
    val isLoading: Boolean,
    val isError: Boolean,
    val isKycGold: Boolean,
    val data: List<InterestDashboardItem>
) : ViewState