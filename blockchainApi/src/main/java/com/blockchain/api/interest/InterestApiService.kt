package com.blockchain.api.interest

import com.blockchain.api.HttpStatus
import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.api.interest.data.InterestAddressDto
import com.blockchain.api.interest.data.InterestAvailableTickersDto
import com.blockchain.api.interest.data.InterestEligibilityDto
import com.blockchain.api.interest.data.InterestRateDto
import com.blockchain.api.interest.data.InterestTickerLimitsDto
import com.blockchain.api.interest.data.InterestWithdrawalBodyDto
import com.blockchain.api.wrapErrorMessage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import retrofit2.HttpException

class InterestApiService internal constructor(
    private val interestApi: InterestApiInterface
) {
    fun getAccountBalances(): Single<Map<String, InterestAccountBalanceDto>> {
        return interestApi.getAccountBalances()
            .map { response ->
                when (response.code()) {
                    HttpStatus.OK -> response.body() ?: emptyMap()
                    HttpStatus.NO_CONTENT -> emptyMap()
                    else -> throw HttpException(response)
                }
            }.wrapErrorMessage()
    }

    fun getAvailableTickersForInterest(): Single<InterestAvailableTickersDto> {
        return interestApi.getAvailableTickersForInterest()
            .wrapErrorMessage()
    }

    fun getTickersEligibility(): Single<Map<String, InterestEligibilityDto>> {
        return interestApi.getTickersEligibility()
            .onErrorReturn { emptyMap() }
    }

    fun getTickersLimits(fiatCurrencyTicker: String): Single<InterestTickerLimitsDto> {
        return interestApi.getTickersLimits(fiatCurrencyTicker = fiatCurrencyTicker)
            .wrapErrorMessage()
    }

    /**
     * If there is no rate for a given asset, this endpoint returns a 204, which must be parsed
     */
    fun getInterestRates(cryptoCurrencyTicker: String): Maybe<InterestRateDto> {
        return interestApi.getInterestRates(cryptoCurrencyTicker = cryptoCurrencyTicker)
            .flatMapMaybe {
                when (it.code()) {
                    200 -> it.body()?.let { Maybe.just(it) } ?: Maybe.empty()
                    204 -> Maybe.empty()
                    else -> Maybe.error(HttpException(it))
                }
            }
            .wrapErrorMessage()
    }

    fun getAddress(cryptoCurrencyTicker: String): Single<InterestAddressDto> {
        return interestApi.getAddress(cryptoCurrencyTicker = cryptoCurrencyTicker)
            .wrapErrorMessage()
    }

    fun performWithdrawal(body: InterestWithdrawalBodyDto): Completable {
        return interestApi.performWithdrawal(body = body)
            .wrapErrorMessage()
    }
}
