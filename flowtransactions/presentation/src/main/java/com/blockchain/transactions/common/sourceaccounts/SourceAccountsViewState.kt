package com.blockchain.transactions.common.sourceaccounts

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.accounts.AccountUiElement

data class SourceAccountsViewState(
    val accountList: DataResource<List<AccountUiElement>>
) : ViewState
