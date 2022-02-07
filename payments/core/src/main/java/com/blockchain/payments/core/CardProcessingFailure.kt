package com.blockchain.payments.core

sealed class CardProcessingFailure(open val throwable: Throwable) {
    data class AuthError(override val throwable: Throwable) : CardProcessingFailure(throwable)
    data class InvalidRequestError(override val throwable: Throwable) : CardProcessingFailure(throwable)
    data class NetworkError(override val throwable: Throwable) : CardProcessingFailure(throwable)
    data class UnknownError(override val throwable: Throwable) : CardProcessingFailure(throwable)
}
