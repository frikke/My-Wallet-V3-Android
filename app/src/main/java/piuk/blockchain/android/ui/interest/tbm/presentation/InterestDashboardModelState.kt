package piuk.blockchain.android.ui.interest.tbm.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class InterestDashboardModelState(
    val isLoadingData: Boolean = false,
    val isError: Boolean = false,
    val isKycGold: Boolean = false,
    val data: List<InterestDashboardItem> = emptyList(),
    val filter: String = ""
) : ModelState