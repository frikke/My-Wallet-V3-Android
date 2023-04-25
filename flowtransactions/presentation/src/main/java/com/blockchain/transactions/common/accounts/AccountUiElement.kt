package com.blockchain.transactions.common.accounts

data class AccountUiElement(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val l2Network: String? = null,
    val valueCrypto: String,
    val valueFiat: String,
    val icon: List<String>
)
