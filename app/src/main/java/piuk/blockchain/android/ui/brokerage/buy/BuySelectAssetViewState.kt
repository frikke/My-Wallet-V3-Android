package piuk.blockchain.android.ui.brokerage.buy

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.nabu.FeatureAccess

data class BuySelectAssetViewState(
    val featureAccess: DataResource<FeatureAccess>
) : ViewState
