package piuk.blockchain.android.ui.interest.tbm.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import piuk.blockchain.android.ui.interest.tbm.presentation.adapter.InterestDashboardItem.InterestAssetInfoItem

data class InterestDashboardViewState(
    val isLoading: Boolean,
    val isError: Boolean,
    val data: List<InterestAssetInfoItem>
) : ViewState {
    fun isDataEmpty() = data.isEmpty()
}