package com.blockchain.transactions.swap.sourceaccounts

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource

sealed interface SwapSourceAccountsIntent : Intent<SwapSourceAccountsModelState> {
    object LoadData : SwapSourceAccountsIntent
    data class AccountSelected(val id: String) : SwapSourceAccountsIntent {
        override fun isValidFor(modelState: SwapSourceAccountsModelState): Boolean {
            return modelState.accountListData is DataResource.Data
        }
    }
}
