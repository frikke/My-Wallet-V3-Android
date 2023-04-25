package com.blockchain.transactions.swap.selecttargetaccount

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.accounts.AccountUiElement
import com.blockchain.walletmode.WalletMode

data class SelectTargetAccountViewState(
    val accountList: DataResource<List<AccountUiElement>>
) : ViewState
