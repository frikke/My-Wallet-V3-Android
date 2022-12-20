package com.blockchain.api.dataremediation.models

import kotlinx.serialization.Serializable

@Serializable
data class QuestionnaireResponse(
    val header: QuestionnaireHeaderResponse?,
    val context: String,
    val nodes: List<QuestionnaireNodeResponse>,
    val blocking: Boolean
)

@Serializable
data class QuestionnaireHeaderResponse(
    val title: String,
    val description: String
)

@Serializable
data class QuestionnaireNodeResponse(
    val id: String,
    val type: String,
    val text: String,
    val children: List<QuestionnaireNodeResponse>?,

    /* node type specific fields */
    // SINGLE_SELECTION and MULTIPLE_SELECTION
    val instructions: String?, // present in SINGLE_SELECTION and MULTIPLE_SELECTION
    val isDropdown: Boolean?, // present in SINGLE_SELECTION and MULTIPLE_SELECTION

    // OPEN_ENDED
    val input: String?,
    val hint: String?,
    val regex: String?,

    // SELECTION
    val checked: Boolean?,
)
