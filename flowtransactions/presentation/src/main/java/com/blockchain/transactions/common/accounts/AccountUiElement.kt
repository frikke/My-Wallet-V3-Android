package com.blockchain.transactions.common.accounts

data class AccountUiElement(
    val ticker: String,
    val assetName: String,
    val l2Network: String? = null,
    val valueCrypto: String,
    val valueFiat: String,
    val icon: List<String>
)
