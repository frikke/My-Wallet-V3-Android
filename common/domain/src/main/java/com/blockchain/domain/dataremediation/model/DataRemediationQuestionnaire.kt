package com.blockchain.domain.dataremediation.model

typealias NodeId = String

sealed class QuestionnaireNode(
    open val id: NodeId,
    open val text: String,
    open val children: List<QuestionnaireNode>
) {

    data class SingleSelection(
        override val id: NodeId,
        override val text: String,
        override val children: List<QuestionnaireNode>,
        val instructions: String,
        val isDropdown: Boolean
    ) : QuestionnaireNode(id, text, children)

    data class MultipleSelection(
        override val id: NodeId,
        override val text: String,
        override val children: List<QuestionnaireNode>,
        val instructions: String
    ) : QuestionnaireNode(id, text, children)

    data class OpenEnded(
        override val id: NodeId,
        override val text: String,
        override val children: List<QuestionnaireNode>,
        val input: String,
        val hint: String
    ) : QuestionnaireNode(id, text, children)

    data class Selection(
        override val id: NodeId,
        override val text: String,
        override val children: List<QuestionnaireNode>,
        val isChecked: Boolean
    ) : QuestionnaireNode(id, text, children)
}

enum class NodeType {
    SINGLE_SELECTION,
    MULTIPLE_SELECTION,
    OPEN_ENDED,
    SELECTION
}
