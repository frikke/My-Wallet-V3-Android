package com.blockchain.core.chains.dynamicselfcustody

data class NonCustodialDerivedAddress(
    val address: String,
    val pubKey: String,
    val includesMemo: Boolean,
    val format: String,
    val default: Boolean,
    val accountIndex: Int
)
