package com.blockchain.api.fees

import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.Serializable

class WithdrawFeesService internal constructor(
    private val feesApi: FeesApi
) {
    fun withdrawFeesAndMinLimit(
        withdrawFeesAndMinRequest: WithdrawFeesAndMinRequest
    ): Single<WithdrawFeesAndMinLimitResponse> =
        feesApi.withdrawalFessAndMinAmount(
            product = withdrawFeesAndMinRequest.product,
            currency = withdrawFeesAndMinRequest.currency,
            fiatCurrency = withdrawFeesAndMinRequest.fiatCurrency,
            amount = withdrawFeesAndMinRequest.amount,
            max = withdrawFeesAndMinRequest.max,
            paymentMethod = withdrawFeesAndMinRequest.paymentMethod
        )
}

@Serializable
data class WithdrawFeesAndMinLimitResponse(
    val minAmount: ExchangedAmountResponse,
    val totalFees: ExchangedAmountResponse,
)

@Serializable
data class ExchangedAmountResponse(
    val amount: AmountResponse
)

@Serializable
data class AmountResponse(
    val currency: String,
    val value: String,
)

@Serializable
data class WithdrawFeesAndMinRequest(
    val product: String = "WALLET",
    val currency: String,
    val fiatCurrency: String,
    val amount: String,
    val max: Boolean,
    val paymentMethod: String,
)
