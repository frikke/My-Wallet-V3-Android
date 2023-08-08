package com.blockchain.api.earn.active

import com.blockchain.api.earn.EarnRewardsEligibilityDto
import com.blockchain.api.earn.active.data.ActiveRewardsAddressDto
import com.blockchain.api.earn.active.data.ActiveRewardsBalanceDto
import com.blockchain.api.earn.active.data.ActiveRewardsLimitsMapDto
import com.blockchain.api.earn.active.data.ActiveRewardsRatesDto
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMapLeft
import com.blockchain.outcome.map

class ActiveRewardsApiService internal constructor(
    private val activeRewardsApi: ActiveRewardsApi
) {
    suspend fun getActiveRewardsRates(): Outcome<Exception, ActiveRewardsRatesDto> =
        activeRewardsApi.getActiveRewardsRates()

    suspend fun getActiveRewardsEligibility(): Outcome<Exception, Map<String, EarnRewardsEligibilityDto>> =
        activeRewardsApi.getActiveRewardsEligibility().flatMapLeft {
            Outcome.Success(emptyMap())
        }

    // Response here can return a 204 (No-Content), in which case, the success of the Outcome would be null;
    // so we catch it and return an empty map if this is the case
    suspend fun getActiveRewardsBalances(): Outcome<Exception, Map<String, ActiveRewardsBalanceDto>> =
        activeRewardsApi.getAccountBalances().map { response ->
            response ?: emptyMap()
        }

    suspend fun getActiveRewardsLimits(fiatTicker: String): Outcome<Exception, ActiveRewardsLimitsMapDto> =
        activeRewardsApi.getTickerLimits(null, fiatTicker)

    suspend fun getActiveRewardsWithdrawals(): Outcome<Exception, List<EarnWithdrawalDto>> =
        activeRewardsApi.getWithdrawalRequests()

    suspend fun getAccountAddress(cryptoTicker: String): Outcome<Exception, ActiveRewardsAddressDto> =
        activeRewardsApi.getAddress(cryptoTicker)
}
