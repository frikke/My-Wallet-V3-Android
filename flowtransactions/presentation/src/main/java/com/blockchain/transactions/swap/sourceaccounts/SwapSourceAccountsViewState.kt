package com.blockchain.transactions.swap.sourceaccounts

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.accounts.AccountUiElement

data class SwapSourceAccountsViewState(
    val accountList: DataResource<List<AccountUiElement>>
) : ViewState
