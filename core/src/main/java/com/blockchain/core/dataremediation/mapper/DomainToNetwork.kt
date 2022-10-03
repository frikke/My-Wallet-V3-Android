package com.blockchain.core.dataremediation.mapper

import com.blockchain.api.dataremediation.models.QuestionnaireHeaderResponse
import com.blockchain.api.dataremediation.models.QuestionnaireNodeResponse
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.domain.dataremediation.model.NodeType
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.dataremediation.model.QuestionnaireHeader
import com.blockchain.domain.dataremediation.model.QuestionnaireNode

fun Questionnaire.toNetwork(): QuestionnaireResponse =
    QuestionnaireResponse(
        header = header?.toNetwork(),
        context = context.toNetwork(),
        nodes = nodes.map { it.toNetwork() },
        blocking = isMandatory
    )

private fun QuestionnaireNode.toNetwork(): QuestionnaireNodeResponse {
    val children = children.map { it.toNetwork() }
    return when (this) {
        is QuestionnaireNode.SingleSelection ->
            QuestionnaireNodeResponse(
                id, NodeType.SINGLE_SELECTION.toNetwork(), text, children, instructions, isDropdown, null, null, null,
                null
            )
        is QuestionnaireNode.MultipleSelection ->
            QuestionnaireNodeResponse(
                id, NodeType.MULTIPLE_SELECTION.toNetwork(), text, children, instructions, null, null, null, null, null
            )
        is QuestionnaireNode.OpenEnded ->
            QuestionnaireNodeResponse(
                id, NodeType.OPEN_ENDED.toNetwork(), text, children, null, null, input, hint, regex?.pattern, null
            )
        is QuestionnaireNode.Selection ->
            QuestionnaireNodeResponse(
                id, NodeType.SELECTION.toNetwork(), text, children, null, null, null, null, null, isChecked
            )
    }
}

private fun NodeType.toNetwork(): String = when (this) {
    NodeType.SINGLE_SELECTION -> "SINGLE_SELECTION"
    NodeType.MULTIPLE_SELECTION -> "MULTIPLE_SELECTION"
    NodeType.OPEN_ENDED -> "OPEN_ENDED"
    NodeType.SELECTION -> "SELECTION"
}

internal fun QuestionnaireContext.toNetwork(): String = when (this) {
    QuestionnaireContext.TIER_TWO_VERIFICATION -> "TIER_TWO_VERIFICATION"
    QuestionnaireContext.FIAT_DEPOSIT -> "FIAT_DEPOSIT"
    QuestionnaireContext.FIAT_WITHDRAW -> "FIAT_WITHDRAW"
    QuestionnaireContext.TRADING -> "TRADING"
}

private fun QuestionnaireHeader.toNetwork(): QuestionnaireHeaderResponse = QuestionnaireHeaderResponse(
    title = title,
    description = description,
)
