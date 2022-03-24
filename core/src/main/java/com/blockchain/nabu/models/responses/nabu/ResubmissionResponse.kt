package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.Serializable

@Serializable
data class ResubmissionResponse(
    val reason: Int? = 0,
    val details: String? = ""
) {
    companion object {
        const val ACCOUNT_RECOVERED_REASON = 1
    }
}
