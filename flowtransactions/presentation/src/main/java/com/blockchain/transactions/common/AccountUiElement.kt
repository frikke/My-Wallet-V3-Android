package com.blockchain.transactions.common

data class AccountUiElement(
    val title: String,
    val subtitle: String,
    val valueCrypto: String,
    val valueFiat: String,
    val icon: List<String>
)
