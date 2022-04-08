package com.blockchain.core.payments.model

sealed class PaymentMethodsError {
    data class RequestFailed(val message: String?) : PaymentMethodsError()
}
