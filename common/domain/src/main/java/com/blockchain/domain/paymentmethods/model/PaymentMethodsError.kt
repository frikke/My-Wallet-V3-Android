package com.blockchain.domain.paymentmethods.model

sealed class PaymentMethodsError {
    data class RequestFailed(val message: String?) : PaymentMethodsError()
}
