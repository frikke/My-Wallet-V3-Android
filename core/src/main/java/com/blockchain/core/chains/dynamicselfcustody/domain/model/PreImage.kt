package com.blockchain.core.chains.dynamicselfcustody.domain.model

import com.blockchain.api.selfcustody.SignatureAlgorithm

data class PreImage(
    val rawPreImage: String,
    val signingKey: String,
    val signatureAlgorithm: SignatureAlgorithm,
    val descriptor: String?
)
