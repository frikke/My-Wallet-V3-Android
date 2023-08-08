package com.dex.data

import com.blockchain.DefiWalletReceiveAddressService
import com.blockchain.api.dex.DexEligibilityApiService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.flatMapData
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.toDataResource
import com.dex.domain.DexEligibilityService
import info.blockchain.balance.CryptoCurrency
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class DexEligibilityRepository(
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val dexEligibilityApiService: DexEligibilityApiService,
    private val coroutineContext: CoroutineContext,
    private val defiWalletReceiveAddressService: DefiWalletReceiveAddressService
) : DexEligibilityService {
    override fun dexEligibility(): Flow<DataResource<FeatureAccess>> {
        val ellipticApiEligibility = flow {
            when (val addressOutcome = defiWalletReceiveAddressService.receiveAddress(CryptoCurrency.ETHER)) {
                is Outcome.Success -> {
                    val eligibilityResponse = dexEligibilityApiService.eligibility(addressOutcome.value.address)
                    emit(eligibilityResponse.toDataResource())
                }

                is Outcome.Failure -> emit(DataResource.Error(addressOutcome.failure))
            }
        }.flowOn(coroutineContext)

        return ellipticApiEligibility.flatMapData {
            if (it.eligible) {
                userFeaturePermissionService.getAccessForFeature(
                    feature = Feature.Dex,
                    freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
            } else
                flowOf(
                    DataResource.Data(
                        FeatureAccess.Blocked(
                            reason = BlockedReason.NotEligible(it.ineligibilityReason)
                        )
                    )
                )
        }.flowOn(coroutineContext)
    }
}
