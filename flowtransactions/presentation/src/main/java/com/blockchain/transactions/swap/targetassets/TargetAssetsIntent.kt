package com.blockchain.transactions.swap.targetassets

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode

sealed interface TargetAssetsIntent : Intent<TargetAssetsModelState> {
    object LoadData : TargetAssetsIntent

    data class FilterSearch(val term: String) : TargetAssetsIntent {
        override fun isValidFor(modelState: TargetAssetsModelState): Boolean {
            return modelState.prices is DataResource.Data
        }
    }

    data class ModeFilterSelected(val selected: WalletMode) : TargetAssetsIntent {
        override fun isValidFor(modelState: TargetAssetsModelState): Boolean {
            return modelState.selectedAssetsModeFilter != selected
        }
    }

    data class AssetSelected(val ticker: String) : TargetAssetsIntent
}
