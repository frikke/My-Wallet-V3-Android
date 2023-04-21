package com.blockchain.transactions.swap.selecttarget

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode

sealed interface SelectTargetIntent : Intent<SelectTargetModelState> {
    object LoadData : SelectTargetIntent

    data class FilterSearch(val term: String) : SelectTargetIntent {
        override fun isValidFor(modelState: SelectTargetModelState): Boolean {
            return modelState.prices is DataResource.Data
        }
    }

    data class ModeFilterSelected(val selected: WalletMode) : SelectTargetIntent {
        override fun isValidFor(modelState: SelectTargetModelState): Boolean {
            return modelState.selectedAssetsModeFilter != selected
        }
    }

    data class AssetSelected(val ticker: String) : SelectTargetIntent
}
