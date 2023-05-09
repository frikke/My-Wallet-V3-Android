package com.blockchain.transactions.swap.targetaccounts

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource

sealed interface TargetAccountsIntent : Intent<TargetAccountsModelState> {
    object LoadData : TargetAccountsIntent

    data class AccountSelected(val id: String) : TargetAccountsIntent {
        override fun isValidFor(modelState: TargetAccountsModelState): Boolean {
            return modelState.accountListData is DataResource.Data
        }
    }
}
