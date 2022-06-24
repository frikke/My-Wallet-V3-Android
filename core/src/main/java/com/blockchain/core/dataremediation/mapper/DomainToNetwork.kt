package com.blockchain.core.dataremediation.mapper

import com.blockchain.api.dataremediation.models.QuestionnaireNodeResponse
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.domain.dataremediation.model.NodeType
import com.blockchain.domain.dataremediation.model.QuestionnaireNode

fun List<QuestionnaireNode>.toNetwork(): QuestionnaireResponse =
    QuestionnaireResponse(this.map { it.toNetwork() })

private fun QuestionnaireNode.toNetwork(): QuestionnaireNodeResponse {
    val children = children.map { it.toNetwork() }
    return when (this) {
        is QuestionnaireNode.SingleSelection ->
            QuestionnaireNodeResponse(
                id, NodeType.SINGLE_SELECTION.toNetwork(), text, children, instructions, isDropdown, null, null, null
            )
        is QuestionnaireNode.MultipleSelection ->
            QuestionnaireNodeResponse(
                id, NodeType.MULTIPLE_SELECTION.toNetwork(), text, children, instructions, null, null, null, null
            )
        is QuestionnaireNode.OpenEnded ->
            QuestionnaireNodeResponse(
                id, NodeType.OPEN_ENDED.toNetwork(), text, children, null, null, input, hint, null
            )
        is QuestionnaireNode.Selection ->
            QuestionnaireNodeResponse(
                id, NodeType.SELECTION.toNetwork(), text, children, null, null, null, null, isChecked
            )
    }
}

private fun NodeType.toNetwork(): String = when (this) {
    NodeType.SINGLE_SELECTION -> "SINGLE_SELECTION"
    NodeType.MULTIPLE_SELECTION -> "MULTIPLE_SELECTION"
    NodeType.OPEN_ENDED -> "OPEN_ENDED"
    NodeType.SELECTION -> "SELECTION"
}
