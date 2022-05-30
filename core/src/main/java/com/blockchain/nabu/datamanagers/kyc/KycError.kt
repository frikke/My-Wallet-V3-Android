package com.blockchain.nabu.datamanagers.kyc

import com.blockchain.nabu.models.responses.nabu.NodeId

enum class KycError {
    REQUEST_FAILED
}

sealed class SubmitQuestionnaireError {
    object RequestFailed : SubmitQuestionnaireError()
    data class InvalidNode(val nodeId: NodeId) : SubmitQuestionnaireError()
}
