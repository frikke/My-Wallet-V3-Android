package com.blockchain.transactions.swap.targetassets

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode

sealed interface SwapTargetAssetsIntent : Intent<SwapTargetAssetsModelState> {
    object LoadData : SwapTargetAssetsIntent

    data class FilterSearch(val term: String) : SwapTargetAssetsIntent {
        override fun isValidFor(modelState: SwapTargetAssetsModelState): Boolean {
            return modelState.prices is DataResource.Data
        }
    }

    data class ModeFilterSelected(val selected: WalletMode) : SwapTargetAssetsIntent {
        override fun isValidFor(modelState: SwapTargetAssetsModelState): Boolean {
            return modelState.selectedAssetsModeFilter != selected
        }
    }

    data class AssetSelected(val ticker: String) : SwapTargetAssetsIntent
}
