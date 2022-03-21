package com.blockchain.api.kyc.models

import kotlinx.serialization.Serializable

@Serializable
data class KycAdditionalInfoResponse(
    val nodes: List<KycAdditionalInfoNodeResponse>
)

@Serializable
data class KycAdditionalInfoNodeResponse(
    val id: String,
    val type: String,
    val text: String,
    val children: List<KycAdditionalInfoNodeResponse>?,

    /* node type specific fields */
    // SINGLE_SELECTION
    val instructions: String?, // present in SINGLE_SELECTION and MULTIPLE_SELECTION
    val isDropdown: Boolean?,

    // OPEN_ENDED
    val input: String?,
    val hint: String?,

    // SELECTION
    val checked: Boolean?
)
