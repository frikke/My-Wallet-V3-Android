package piuk.blockchain.android.ui.brokerage.buy

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.nabu.FeatureAccess

data class BuySelectAssetModelState(
    val featureAccess: DataResource<FeatureAccess> = DataResource.Loading,
    val showTopMovers: Boolean = false
) : ModelState
