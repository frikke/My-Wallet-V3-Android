package com.blockchain.core.interest.data.datasources

import com.blockchain.api.services.NabuUserService
import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.core.interest.domain.model.InterestEligibility
import com.blockchain.nabu.Authenticator
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class InterestEligibilityTimedCache(
    private val authenticator: Authenticator,
    private val assetCatalogue: AssetCatalogue,
    private val service: NabuUserService
) {

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = ::refresh
    )

    private fun refresh(): Single<Map<AssetInfo, InterestEligibility>> =
        authenticator.authenticate { token ->
            service.getInterestEligibility(token.authHeader)
                .map { interestEligibility ->
                    assetCatalogue.supportedCustodialAssets.associateWith { asset ->
                        interestEligibility.getEligibleFor(asset.networkTicker).let { eligibilityResponse ->
                            if (eligibilityResponse.isEligible) {
                                InterestEligibility.Eligible
                            } else {
                                eligibilityResponse.reason.toIneligibilityReason()
                            }
                        }
                    }
                }
        }

    fun cached(): Single<Map<AssetInfo, InterestEligibility>> = cache.getCachedSingle()

    companion object {
        private const val CACHE_LIFETIME = 20L

        private const val UNSUPPORTED_REGION = "UNSUPPORTED_REGION"
        private const val TIER_TOO_LOW = "TIER_TOO_LOW"
        private const val INVALID_ADDRESS = "INVALID_ADDRESS"
        private const val ELIGIBLE = "NONE"
    }

    private fun String.toIneligibilityReason(): InterestEligibility.Ineligible =
        when {
            this.isEmpty() -> InterestEligibility.Ineligible.NONE
            this == ELIGIBLE -> InterestEligibility.Ineligible.NONE
            this == UNSUPPORTED_REGION -> InterestEligibility.Ineligible.REGION
            this == INVALID_ADDRESS -> InterestEligibility.Ineligible.REGION
            this == TIER_TOO_LOW -> InterestEligibility.Ineligible.KYC_TIER
            else -> InterestEligibility.Ineligible.OTHER
        }
}
