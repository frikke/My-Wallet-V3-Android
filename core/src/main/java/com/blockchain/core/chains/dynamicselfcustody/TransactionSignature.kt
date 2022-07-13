package com.blockchain.core.chains.dynamicselfcustody

import com.blockchain.api.selfcustody.SignatureAlgorithm

data class TransactionSignature(
    val preImage: String,
    val signingKey: String,
    val signatureAlgorithm: SignatureAlgorithm,
    val signature: String
)
