package com.blockchain.domain.paymentmethods.model

data class BankProviderAccountAttributes(
    val providerAccountId: String? = null,
    val accountId: String? = null,
    val institutionId: String? = null,
    val callback: String? = null
)
