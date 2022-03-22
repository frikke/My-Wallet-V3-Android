package com.blockchain.nabu.datamanagers.kyc

import com.blockchain.api.kyc.models.KycAdditionalInfoNodeResponse
import com.blockchain.api.kyc.models.KycAdditionalInfoResponse
import com.blockchain.nabu.models.responses.nabu.KycAdditionalInfoNode
import com.blockchain.nabu.models.responses.nabu.NodeType

fun List<KycAdditionalInfoNode>.toNetwork(): KycAdditionalInfoResponse =
    KycAdditionalInfoResponse(this.map { it.toNetwork() })

private fun KycAdditionalInfoNode.toNetwork(): KycAdditionalInfoNodeResponse {
    val children = children.map { it.toNetwork() }
    return when (this) {
        is KycAdditionalInfoNode.SingleSelection ->
            KycAdditionalInfoNodeResponse(
                id, NodeType.SINGLE_SELECTION.toNetwork(), text, children, instructions, isDropdown, null, null, null
            )
        is KycAdditionalInfoNode.MultipleSelection ->
            KycAdditionalInfoNodeResponse(
                id, NodeType.MULTIPLE_SELECTION.toNetwork(), text, children, instructions, null, null, null, null
            )
        is KycAdditionalInfoNode.OpenEnded ->
            KycAdditionalInfoNodeResponse(
                id, NodeType.OPEN_ENDED.toNetwork(), text, children, null, null, input, hint, null
            )
        is KycAdditionalInfoNode.Selection ->
            KycAdditionalInfoNodeResponse(
                id, NodeType.SELECTION.toNetwork(), text, children, null, null, null, null, isChecked
            )
    }
}

fun KycAdditionalInfoResponse.toDomain(): List<KycAdditionalInfoNode> =
    nodes.mapNotNull { it.toDomain() }

private fun KycAdditionalInfoNodeResponse.toDomain(): KycAdditionalInfoNode? {
    val type = type.toNodeType() ?: return null
    val children = children.orEmpty().mapNotNull { it.toDomain() }
    return when (type) {
        NodeType.SINGLE_SELECTION ->
            KycAdditionalInfoNode.SingleSelection(id, text, children, instructions.orEmpty(), isDropdown ?: false)
        NodeType.MULTIPLE_SELECTION ->
            KycAdditionalInfoNode.MultipleSelection(id, text, children, instructions.orEmpty())
        NodeType.OPEN_ENDED -> KycAdditionalInfoNode.OpenEnded(id, text, children, input.orEmpty(), hint.orEmpty())
        NodeType.SELECTION -> KycAdditionalInfoNode.Selection(id, text, children, checked ?: false)
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
