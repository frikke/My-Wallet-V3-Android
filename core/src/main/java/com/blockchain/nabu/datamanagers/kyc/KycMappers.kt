package com.blockchain.nabu.datamanagers.kyc

import com.blockchain.api.kyc.models.KycQuestionnaireNodeResponse
import com.blockchain.api.kyc.models.KycQuestionnaireResponse
import com.blockchain.nabu.models.responses.nabu.KycQuestionnaireNode
import com.blockchain.nabu.models.responses.nabu.NodeType

fun List<KycQuestionnaireNode>.toNetwork(): KycQuestionnaireResponse =
    KycQuestionnaireResponse(this.map { it.toNetwork() })

private fun KycQuestionnaireNode.toNetwork(): KycQuestionnaireNodeResponse {
    val children = children.map { it.toNetwork() }
    return when (this) {
        is KycQuestionnaireNode.SingleSelection ->
            KycQuestionnaireNodeResponse(
                id, NodeType.SINGLE_SELECTION.toNetwork(), text, children, instructions, isDropdown, null, null, null
            )
        is KycQuestionnaireNode.MultipleSelection ->
            KycQuestionnaireNodeResponse(
                id, NodeType.MULTIPLE_SELECTION.toNetwork(), text, children, instructions, null, null, null, null
            )
        is KycQuestionnaireNode.OpenEnded ->
            KycQuestionnaireNodeResponse(
                id, NodeType.OPEN_ENDED.toNetwork(), text, children, null, null, input, hint, null
            )
        is KycQuestionnaireNode.Selection ->
            KycQuestionnaireNodeResponse(
                id, NodeType.SELECTION.toNetwork(), text, children, null, null, null, null, isChecked
            )
    }
}

fun KycQuestionnaireResponse.toDomain(): List<KycQuestionnaireNode> =
    nodes.mapNotNull { it.toDomain() }

private fun KycQuestionnaireNodeResponse.toDomain(): KycQuestionnaireNode? {
    val type = type.toNodeType() ?: return null
    val children = children.orEmpty().mapNotNull { it.toDomain() }
    return when (type) {
        NodeType.SINGLE_SELECTION ->
            KycQuestionnaireNode.SingleSelection(id, text, children, instructions.orEmpty(), isDropdown ?: false)
        NodeType.MULTIPLE_SELECTION ->
            KycQuestionnaireNode.MultipleSelection(id, text, children, instructions.orEmpty())
        NodeType.OPEN_ENDED -> KycQuestionnaireNode.OpenEnded(id, text, children, input.orEmpty(), hint.orEmpty())
        NodeType.SELECTION -> KycQuestionnaireNode.Selection(id, text, children, checked ?: false)
    }
}

private fun NodeType.toNetwork(): String = when (this) {
    NodeType.SINGLE_SELECTION -> "SINGLE_SELECTION"
    NodeType.MULTIPLE_SELECTION -> "MULTIPLE_SELECTION"
    NodeType.OPEN_ENDED -> "OPEN_ENDED"
    NodeType.SELECTION -> "SELECTION"
}

private fun String.toNodeType(): NodeType? = when (this) {
    "SINGLE_SELECTION" -> NodeType.SINGLE_SELECTION
    "MULTIPLE_SELECTION" -> NodeType.MULTIPLE_SELECTION
    "OPEN_ENDED" -> NodeType.OPEN_ENDED
    "SELECTION" -> NodeType.SELECTION
    else -> null
}
