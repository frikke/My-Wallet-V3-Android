package com.blockchain.transactions.common

import com.blockchain.componentlib.tablerow.custom.StackedIcon

data class AccountUiElement(
    val title: String,
    val subtitle: String,
    val valueCrypto: String,
    val valueFiat: String,
    val icon: List<String>
)