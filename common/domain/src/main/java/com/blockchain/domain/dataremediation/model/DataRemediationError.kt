package com.blockchain.domain.dataremediation.model

enum class DataRemediationError {
    REQUEST_FAILED
}

sealed class SubmitQuestionnaireError {
    object RequestFailed : SubmitQuestionnaireError()
    data class InvalidNode(val nodeId: NodeId) : SubmitQuestionnaireError()
}
