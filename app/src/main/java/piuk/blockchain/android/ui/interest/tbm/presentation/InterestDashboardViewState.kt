package piuk.blockchain.android.ui.interest.tbm.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.presentation.adapter.InterestDashboardItem

data class InterestDashboardViewState(
    val isLoading: Boolean,
    val isError: Boolean,
    val isKycGold: Boolean,
    val data: List<InterestDashboardItem>
) : ViewState