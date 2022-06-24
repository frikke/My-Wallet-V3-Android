package com.blockchain.core.dataremediation.mapper

import com.blockchain.api.dataremediation.models.QuestionnaireNodeResponse
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.domain.dataremediation.model.NodeType
import com.blockchain.domain.dataremediation.model.QuestionnaireNode

fun QuestionnaireResponse.toDomain(): List<QuestionnaireNode> =
    nodes.mapNotNull { it.toDomain() }

private fun QuestionnaireNodeResponse.toDomain(): QuestionnaireNode? {
    val type = type.toNodeType() ?: return null
    val children = children.orEmpty().mapNotNull { it.toDomain() }
    return when (type) {
        NodeType.SINGLE_SELECTION ->
            QuestionnaireNode.SingleSelection(id, text, children, instructions.orEmpty(), isDropdown ?: false)
        NodeType.MULTIPLE_SELECTION ->
            QuestionnaireNode.MultipleSelection(id, text, children, instructions.orEmpty())
        NodeType.OPEN_ENDED -> QuestionnaireNode.OpenEnded(id, text, children, input.orEmpty(), hint.orEmpty())
        NodeType.SELECTION -> QuestionnaireNode.Selection(id, text, children, checked ?: false)
    }
}

private fun String.toNodeType(): NodeType? = when (this) {
    "SINGLE_SELECTION" -> NodeType.SINGLE_SELECTION
    "MULTIPLE_SELECTION" -> NodeType.MULTIPLE_SELECTION
    "OPEN_ENDED" -> NodeType.OPEN_ENDED
    "SELECTION" -> NodeType.SELECTION
    else -> null
}
