package com.blockchain.core.eligibility.mapper

import com.blockchain.domain.eligibility.model.GetRegionScope

fun GetRegionScope.toNetwork(): String? = when (this) {
    GetRegionScope.Signup -> "SIGNUP"
    GetRegionScope.Kyc -> "kyc"
    GetRegionScope.None -> null
}
