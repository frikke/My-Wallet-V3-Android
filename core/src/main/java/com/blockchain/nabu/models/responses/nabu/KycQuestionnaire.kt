package com.blockchain.nabu.models.responses.nabu

typealias NodeId = String

sealed class KycQuestionnaireNode(
    open val id: NodeId,
    open val text: String,
    open val children: List<KycQuestionnaireNode>
) {

    data class SingleSelection(
        override val id: NodeId,
        override val text: String,
        override val children: List<KycQuestionnaireNode>,
        val instructions: String,
        val isDropdown: Boolean
    ) : KycQuestionnaireNode(id, text, children)

    data class MultipleSelection(
        override val id: NodeId,
        override val text: String,
        override val children: List<KycQuestionnaireNode>,
        val instructions: String
    ) : KycQuestionnaireNode(id, text, children)

    data class OpenEnded(
        override val id: NodeId,
        override val text: String,
        override val children: List<KycQuestionnaireNode>,
        val input: String,
        val hint: String
    ) : KycQuestionnaireNode(id, text, children)

    data class Selection(
        override val id: NodeId,
        override val text: String,
        override val children: List<KycQuestionnaireNode>,
        val isChecked: Boolean
    ) : KycQuestionnaireNode(id, text, children)
}

enum class NodeType {
    SINGLE_SELECTION,
    MULTIPLE_SELECTION,
    OPEN_ENDED,
    SELECTION
}
