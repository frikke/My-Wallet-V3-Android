package com.blockchain.transactions.swap.selecttargetaccount

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.transactions.swap.selectsource.SelectSourceModelState
import com.blockchain.transactions.swap.selecttarget.SelectTargetIntent
import com.blockchain.transactions.swap.selecttarget.SelectTargetModelState
import com.blockchain.walletmode.WalletMode

sealed interface SelectTargetAccountIntent : Intent<SelectTargetAccountModelState> {
    object LoadData : SelectTargetAccountIntent

    data class AccountSelected(val id: String) : SelectTargetAccountIntent {
        override fun isValidFor(modelState: SelectTargetAccountModelState): Boolean {
            return modelState.accountListData is DataResource.Data
        }
    }
}
