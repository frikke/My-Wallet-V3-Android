package com.blockchain.api.dataremediation.models

import kotlinx.serialization.Serializable

@Serializable
data class QuestionnaireResponse(
    val nodes: List<QuestionnaireNodeResponse>
)

@Serializable
data class QuestionnaireNodeResponse(
    val id: String,
    val type: String,
    val text: String,
    val children: List<QuestionnaireNodeResponse>?,

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
