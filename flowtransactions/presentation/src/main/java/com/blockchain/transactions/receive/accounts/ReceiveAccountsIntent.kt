package com.blockchain.transactions.receive.accounts

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface ReceiveAccountsIntent : Intent<ReceiveAccountsModelState> {
    object LoadData : ReceiveAccountsIntent
    data class AccountSelected(val id: String) : ReceiveAccountsIntent
    data class Search(val term: String) : ReceiveAccountsIntent
}
