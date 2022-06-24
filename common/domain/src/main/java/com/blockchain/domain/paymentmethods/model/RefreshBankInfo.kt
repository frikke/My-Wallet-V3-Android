package com.blockchain.domain.paymentmethods.model

data class RefreshBankInfo(
    val partner: BankPartner?,
    val id: String,
    val linkToken: String,
    val linkUrl: String,
    val tokenExpiresAt: String,
)
