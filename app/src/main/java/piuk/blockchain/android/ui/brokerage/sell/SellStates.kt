package piuk.blockchain.android.ui.brokerage.sell

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.sell.domain.SellEligibility
import com.blockchain.data.DataResource

data class SellViewState(
    val data: DataResource<SellEligibility>,
    val showLoader: Boolean
) : ViewState

data class SellModelState(
    val data: DataResource<SellEligibility>,
    val shouldShowLoading: Boolean
) : ModelState

sealed class SellNavigation : NavigationEvent
