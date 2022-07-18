package com.blockchain.api.services

import com.blockchain.api.HttpStatus
import com.blockchain.api.interest.InterestApiInterface
import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.api.wrapErrorMessage
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import retrofit2.HttpException

@Serializable
data class InterestBalanceDetails(
    val assetTicker: String,
    val totalBalance: @Contextual BigInteger,
    val pendingInterest: @Contextual BigInteger,
    val pendingDeposit: @Contextual BigInteger,
    val totalInterest: @Contextual BigInteger,
    val pendingWithdrawal: @Contextual BigInteger,
    val lockedBalance: @Contextual BigInteger
)

typealias InterestBalanceDetailsList = List<InterestBalanceDetails>

class InterestApiService internal constructor(
    private val api: InterestApiInterface
) {
    fun getAllInterestAccountBalances(
        authHeader: String
    ): Single<InterestBalanceDetailsList> =
        api.getAllInterestAccountBalances(authHeader)
            .map { response ->
                when (response.code()) {
                    HttpStatus.OK -> response.body() ?: emptyMap()
                    HttpStatus.NO_CONTENT -> emptyMap()
                    else -> throw HttpException(response)
                }
            }.map { balanceMap ->
                balanceMap.mapValues { (assetTicker, balance) ->
                    balance.toBalanceDetails(assetTicker)
                }.values.toList()
            }.wrapErrorMessage()
}

private fun InterestAccountBalanceDto.toBalanceDetails(assetTicker: String): InterestBalanceDetails =
    InterestBalanceDetails(
        assetTicker = assetTicker,
        totalBalance = totalBalance.toBigInteger(),
        pendingInterest = pendingInterest.toBigInteger(),
        pendingDeposit = pendingDeposit.toBigInteger(),
        pendingWithdrawal = pendingWithdrawal.toBigInteger(),
        totalInterest = totalInterest.toBigInteger(),
        lockedBalance = lockedBalance.toBigInteger()
    )
