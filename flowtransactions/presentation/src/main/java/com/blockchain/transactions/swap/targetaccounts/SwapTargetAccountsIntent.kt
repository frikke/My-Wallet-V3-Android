package com.blockchain.transactions.swap.targetaccounts

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource

sealed interface SwapTargetAccountsIntent : Intent<SwapTargetAccountsModelState> {
    object LoadData : SwapTargetAccountsIntent

    data class AccountSelected(val id: String) : SwapTargetAccountsIntent {
        override fun isValidFor(modelState: SwapTargetAccountsModelState): Boolean {
            return modelState.accountListData is DataResource.Data
        }
    }
}
