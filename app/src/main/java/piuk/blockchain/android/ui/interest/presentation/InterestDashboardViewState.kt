package piuk.blockchain.android.ui.interest.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import piuk.blockchain.android.ui.interest.domain.model.InterestAsset

data class InterestDashboardViewState(
    val isLoading: Boolean,
    val isError: Boolean,
    val isKycGold: Boolean,
    val data: List<InterestAsset>
) : ViewState
