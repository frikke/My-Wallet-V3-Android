package com.blockchain.domain.paymentmethods.model

data class AliasInfo(
    val bankName: String?,
    val alias: String?,
    val accountHolder: String?,
    val accountType: String?,
    val cbu: String?,
    val cuil: String?
)
