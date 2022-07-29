package com.blockchain.api.interest

import com.blockchain.api.HttpStatus
import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.api.interest.data.InterestAvailableTickersDto
import com.blockchain.api.wrapErrorMessage
import io.reactivex.rxjava3.core.Single
import retrofit2.HttpException

class InterestApiService internal constructor(
    private val interestApi: InterestApiInterface
) {
    fun getAccountBalances(authHeader: String): Single<Map<String, InterestAccountBalanceDto>> {
        return interestApi.getAccountBalances(authHeader)
            .map { response ->
                when (response.code()) {
                    HttpStatus.OK -> response.body() ?: emptyMap()
                    HttpStatus.NO_CONTENT -> emptyMap()
                    else -> throw HttpException(response)
                }
            }.wrapErrorMessage()
    }

    fun getAvailableTickersForInterest(authHeader: String): Single<InterestAvailableTickersDto> {
        return interestApi.getAvailableTickersForInterest(authorization = authHeader)
            .wrapErrorMessage()
    }
}
