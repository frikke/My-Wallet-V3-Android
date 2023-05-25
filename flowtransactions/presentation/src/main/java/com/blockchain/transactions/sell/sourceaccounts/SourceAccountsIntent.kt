package com.blockchain.transactions.sell.sourceaccounts

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource

sealed interface SourceAccountsIntent : Intent<SourceAccountsModelState> {
    object LoadData : SourceAccountsIntent
    data class AccountSelected(val id: String) : SourceAccountsIntent {
        override fun isValidFor(modelState: SourceAccountsModelState): Boolean {
            return modelState.accountListData is DataResource.Data
        }
    }
}
