package com.blockchain.api.earn.passive

import com.blockchain.api.HttpStatus
import com.blockchain.api.earn.EarnRewardsEligibilityResponseDto
import com.blockchain.api.earn.passive.data.InterestAccountBalanceDto
import com.blockchain.api.earn.passive.data.InterestAddressDto
import com.blockchain.api.earn.passive.data.InterestAvailableTickersDto
import com.blockchain.api.earn.passive.data.InterestRateDto
import com.blockchain.api.earn.passive.data.InterestRatesDto
import com.blockchain.api.earn.passive.data.InterestTickerLimitsDto
import com.blockchain.api.earn.passive.data.InterestWithdrawalBodyDto
import com.blockchain.api.wrapErrorMessage
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import retrofit2.HttpException

class InterestApiService internal constructor(
    private val interestApi: InterestApiInterface
) {
    fun getAccountBalances(): Single<Map<String, InterestAccountBalanceDto>> {
        return interestApi.getAccountBalances()
            .flatMap { response ->
                when (response.code()) {
                    HttpStatus.OK -> Single.just(response.body() ?: emptyMap())
                    HttpStatus.NO_CONTENT -> Single.just(emptyMap())
                    else -> Single.error(HttpException(response))
                }
            }.wrapErrorMessage()
    }

    fun getAvailableTickersForInterest(): Single<InterestAvailableTickersDto> {
        return interestApi.getAvailableTickersForInterest()
            .wrapErrorMessage()
    }

    fun getTickersEligibility(): Single<EarnRewardsEligibilityResponseDto> {
        return interestApi.getTickersEligibility()
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

    suspend fun getAllInterestRates(): Outcome<Exception, InterestRatesDto> =
        interestApi.getAllInterestRates()

    fun getAddress(cryptoCurrencyTicker: String): Single<InterestAddressDto> {
        return interestApi.getAddress(cryptoCurrencyTicker = cryptoCurrencyTicker)
            .wrapErrorMessage()
    }

    fun performWithdrawal(body: InterestWithdrawalBodyDto): Completable {
        return interestApi.performWithdrawal(body = body)
            .wrapErrorMessage()
    }
}
