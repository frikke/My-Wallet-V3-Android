package com.blockchain.domain.eligibility.model

sealed class EligibilityError {
    data class RequestFailed(val message: String?) : EligibilityError()
}
