package com.blockchain.transactions.swap.targetaccounts

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.accounts.AccountUiElement

data class SwapTargetAccountsViewState(
    val accountList: DataResource<List<AccountUiElement>>
) : ViewState
