package com.blockchain.nabu.models.responses.nabu

typealias NodeId = String

sealed class KycAdditionalInfoNode(
    open val id: NodeId,
    open val text: String,
    open val children: List<KycAdditionalInfoNode>
) {

    data class SingleSelection(
        override val id: NodeId,
        override val text: String,
        override val children: List<KycAdditionalInfoNode>,
        val instructions: String,
        val isDropdown: Boolean
    ) : KycAdditionalInfoNode(id, text, children)

    data class MultipleSelection(
        override val id: NodeId,
        override val text: String,
        override val children: List<KycAdditionalInfoNode>,
        val instructions: String
    ) : KycAdditionalInfoNode(id, text, children)

    data class OpenEnded(
        override val id: NodeId,
        override val text: String,
        override val children: List<KycAdditionalInfoNode>,
        val input: String,
        val hint: String
    ) : KycAdditionalInfoNode(id, text, children)

    data class Selection(
        override val id: NodeId,
        override val text: String,
        override val children: List<KycAdditionalInfoNode>,
        val isChecked: Boolean
    ) : KycAdditionalInfoNode(id, text, children)
}

enum class NodeType {
    SINGLE_SELECTION,
    MULTIPLE_SELECTION,
    OPEN_ENDED,
    SELECTION
}
