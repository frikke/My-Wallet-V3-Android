package com.blockchain.kycproviders.prove.presentation

import kotlin.jvm.Throws

interface ProveAuthSDK {
    @Throws(Exception::class)
    fun isAuthenticationPossible()
    fun authenticate(): Boolean
}

data class ProveAuthResult(val mobileNumber: String)
