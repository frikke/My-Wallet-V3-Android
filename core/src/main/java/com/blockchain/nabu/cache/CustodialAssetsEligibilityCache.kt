package com.blockchain.nabu.cache

import com.blockchain.api.services.NabuUserService
import com.blockchain.caching.TimedCacheRequest
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.datamanagers.repositories.interest.AssetInterestEligibility
import com.blockchain.nabu.datamanagers.repositories.interest.Eligibility
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetCatalogue
import io.reactivex.rxjava3.core.Single

class CustodialAssetsEligibilityCache(
    private val authenticator: Authenticator,
    private val assetCatalogue: AssetCatalogue,
    private val service: NabuUserService
) {

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = ::refresh
    )

    private fun refresh(): Single<List<AssetInterestEligibility>> =
        authenticator.authenticate { token ->
            service.getInterestEligibility(token.authHeader)
                .map { response ->
                    assetCatalogue.supportedCustodialAssets
                        .map { asset ->
                            val eligible = response.getEligibleFor(asset.networkTicker)
                            AssetInterestEligibility(
                                asset,
                                Eligibility(
                                    eligible = eligible.isEligible,
                                    ineligibilityReason = eligible.reason.toReason()
                                )
                            )
                        }
                }
        }

    fun cached(): Single<List<AssetInterestEligibility>> =
        cache.getCachedSingle()

    companion object {
        private const val CACHE_LIFETIME = 20L

        const val UNSUPPORTED_REGION = "UNSUPPORTED_REGION"
        const val TIER_TOO_LOW = "TIER_TOO_LOW"
        const val INVALID_ADDRESS = "INVALID_ADDRESS"
        const val ELIGIBLE = "NONE"
    }

    private fun String.toReason(): IneligibilityReason =
        when {
            this.isEmpty() -> IneligibilityReason.NONE
            this == ELIGIBLE -> IneligibilityReason.NONE
            this == UNSUPPORTED_REGION -> IneligibilityReason.REGION
            this == INVALID_ADDRESS -> IneligibilityReason.REGION
            this == TIER_TOO_LOW -> IneligibilityReason.KYC_TIER
            else -> IneligibilityReason.OTHER
        }
}
