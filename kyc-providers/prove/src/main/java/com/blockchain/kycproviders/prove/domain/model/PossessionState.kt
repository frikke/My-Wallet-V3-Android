package com.blockchain.kycproviders.prove.domain.model

sealed class PossessionState {
    object Unverified : PossessionState()
    data class Verified(val mobileNumber: String) : PossessionState()
    object Failed : PossessionState()
}
