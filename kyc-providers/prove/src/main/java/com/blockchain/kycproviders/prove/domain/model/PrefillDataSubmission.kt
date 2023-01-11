package com.blockchain.kycproviders.prove.domain.model

data class PrefillDataSubmission(
    val firstName: String,
    val lastName: String,
    val address: Address,
    val dob: String, // ISO 8601
    val mobileNumber: String,
)
