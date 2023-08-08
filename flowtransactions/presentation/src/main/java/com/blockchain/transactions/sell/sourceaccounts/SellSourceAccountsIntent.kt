package com.blockchain.transactions.sell.sourceaccounts

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource

sealed interface SellSourceAccountsIntent : Intent<SellSourceAccountsModelState> {
    object LoadData : SellSourceAccountsIntent
    data class AccountSelected(val id: String) : SellSourceAccountsIntent {
        override fun isValidFor(modelState: SellSourceAccountsModelState): Boolean {
            return modelState.accountListData is DataResource.Data
        }
    }
}
