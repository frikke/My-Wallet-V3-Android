package com.blockchain.domain.dataremediation.model

enum class DataRemediationError {
    REQUEST_FAILED
}

sealed class SubmitQuestionnaireError {
    data class RequestFailed(val message: String?) : SubmitQuestionnaireError()
    data class InvalidNode(val nodeId: NodeId) : SubmitQuestionnaireError()
}
