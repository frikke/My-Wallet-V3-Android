package piuk.blockchain.android.ui.interest.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import piuk.blockchain.android.ui.interest.domain.model.InterestAsset

data class InterestDashboardModelState(
    val isLoadingData: Boolean = false,
    val isError: Boolean = false,
    val isKycGold: Boolean = false,
    val data: List<InterestAsset> = emptyList(),
    val filter: String = ""
) : ModelState
