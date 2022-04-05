package piuk.blockchain.android.ui.interest.tbm.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.presentation.adapter.InterestDashboardItem

data class InterestDashboardModelState(
    val isLoadingData: Boolean = false,
    val isError: Boolean = false,
    val isKycGold: Boolean = false,
    val data: List<InterestDashboardItem> = emptyList()
) : ModelState