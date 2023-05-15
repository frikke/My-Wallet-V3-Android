package com.blockchain.core.dataremediation.mapper

import com.blockchain.api.NabuApiException
import com.blockchain.api.dataremediation.models.QuestionnaireHeaderResponse
import com.blockchain.api.dataremediation.models.QuestionnaireNodeResponse
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.domain.dataremediation.model.NodeId
import com.blockchain.domain.dataremediation.model.NodeType
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.dataremediation.model.QuestionnaireHeader
import com.blockchain.domain.dataremediation.model.QuestionnaireNode
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError

fun QuestionnaireResponse.toDomain(): Questionnaire = Questionnaire(
    header = header?.toDomain(),
    context = context.toContext(),
    nodes = nodes.mapNotNull { it.toDomain() },
    isMandatory = blocking
)

private fun QuestionnaireNodeResponse.toDomain(): QuestionnaireNode? {
    val type = type.toNodeType() ?: return null
    val children = children.orEmpty().mapNotNull { it.toDomain() }
    return when (type) {
        NodeType.SINGLE_SELECTION ->
            QuestionnaireNode.SingleSelection(id, text, children, instructions.orEmpty(), isDropdown ?: false)
        NodeType.MULTIPLE_SELECTION -> {
            // There's currently no support for children of Selection inside MultipleSelection dropdowns
            val sanitizedChildren = if (isDropdown == true) {
                children.map { child ->
                    if (child is QuestionnaireNode.Selection) {
                        child.copy(children = emptyList())
                    } else {
                        child
                    }
                }
            } else {
                children
            }
            QuestionnaireNode.MultipleSelection(
                id,
                text,
                sanitizedChildren,
                instructions.orEmpty(),
                isDropdown ?: false
            )
        }
        NodeType.OPEN_ENDED ->
            QuestionnaireNode.OpenEnded(id, text, children, input.orEmpty(), hint.orEmpty(), regex?.let { Regex(it) })
        NodeType.SELECTION -> QuestionnaireNode.Selection(id, text, children, checked ?: false)
    }
}

private fun String.toContext(): QuestionnaireContext = QuestionnaireContext.values().find {
    val key = it.toNetwork()
    this.equals(key, ignoreCase = true)
    // Fallback, shouldn't occur as we're asking the backend for a specific context in the GET
} ?: QuestionnaireContext.TIER_TWO_VERIFICATION

private fun String.toNodeType(): NodeType? = when (this) {
    "SINGLE_SELECTION" -> NodeType.SINGLE_SELECTION
    "MULTIPLE_SELECTION" -> NodeType.MULTIPLE_SELECTION
    "OPEN_ENDED" -> NodeType.OPEN_ENDED
    "SELECTION" -> NodeType.SELECTION
    else -> null
}

private fun QuestionnaireHeaderResponse.toDomain(): QuestionnaireHeader = QuestionnaireHeader(
    title = title,
    description = description
)

internal fun Exception.toError(): SubmitQuestionnaireError {
    val nodeId = this.tryParseNodeIdFromApiError()
    return if (nodeId != null) {
        SubmitQuestionnaireError.InvalidNode(nodeId)
    } else {
        SubmitQuestionnaireError.RequestFailed(
            message = (this as? NabuApiException)?.getErrorDescription().takeIf { !it.isNullOrBlank() }
                ?: this.message
        )
    }
}

private fun Exception.tryParseNodeIdFromApiError(): NodeId? {
    val errorDescription =
        (this as? NabuApiException)?.getErrorDescription()?.takeIf { desc -> desc.count { it == '#' } == 2 }
            ?: return null

    val indexOfStart = errorDescription.indexOf('#') + 1
    val indexOfEnd = errorDescription.substring(indexOfStart).indexOf('#')
    return errorDescription.substring(indexOfStart, indexOfStart + indexOfEnd)
}
