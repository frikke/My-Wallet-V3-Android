package com.blockchain.componentlib.card

data class CardButton(
    val text: String,
    val onClick: () -> Unit = {}
)
