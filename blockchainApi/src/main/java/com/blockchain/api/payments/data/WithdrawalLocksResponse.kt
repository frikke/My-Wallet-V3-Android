@file:UseSerializers(BigIntSerializer::class)
package com.blockchain.api.payments.data

import com.blockchain.api.serializers.BigIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class WithdrawalLocksResponse(
    @SerialName("locks")
    val locks: List<LockPeriod>,
    @SerialName("totalLocked")
    val totalLocked: LocalCurrencyAmount
)

@Serializable
data class LockPeriod(
    @SerialName("amount")
    val localCurrencyAmount: LocalCurrencyAmount,
    @SerialName("expiresAt")
    val expiresAt: String
)

@Serializable
data class LocalCurrencyAmount(
    @SerialName("currency")
    val currency: String,
    @SerialName("amount")
    val amount: String
)
