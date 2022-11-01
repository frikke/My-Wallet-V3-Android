package piuk.blockchain.android.ui.brokerage.sell

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.core.sell.domain.SellEligibility
import com.blockchain.data.DataResource

data class SellViewState(
    val sellEligibility: DataResource<SellEligibility>,
    val showLoader: Boolean,
    val supportedAccountList: DataResource<List<CryptoAccount>> = DataResource.Loading
) : ViewState

data class SellModelState(
    val sellEligibility: DataResource<SellEligibility> = DataResource.Loading,
    val shouldShowLoading: Boolean = true,
    val supportedAccountList: DataResource<List<CryptoAccount>> = DataResource.Loading,
    val searchTerm: String = ""
) : ModelState

sealed class SellNavigation : NavigationEvent
