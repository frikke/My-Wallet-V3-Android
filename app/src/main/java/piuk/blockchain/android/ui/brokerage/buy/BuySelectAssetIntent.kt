package piuk.blockchain.android.ui.brokerage.buy

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetInfo

sealed interface BuySelectAssetIntent : Intent<BuySelectAssetModelState> {
    object LoadEligibility : BuySelectAssetIntent
    data class AssetClicked(val assetInfo: AssetInfo) : BuySelectAssetIntent {
        override fun isValidFor(modelState: BuySelectAssetModelState): Boolean {
            return modelState.featureAccess is DataResource.Data
        }
    }
}
