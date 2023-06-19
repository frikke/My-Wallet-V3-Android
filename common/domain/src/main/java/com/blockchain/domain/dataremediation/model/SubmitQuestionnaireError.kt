package com.blockchain.domain.dataremediation.model

sealed class SubmitQuestionnaireError {
    data class RequestFailed(val message: String?) : SubmitQuestionnaireError()
    data class InvalidNode(val nodeId: NodeId) : SubmitQuestionnaireError()
}
