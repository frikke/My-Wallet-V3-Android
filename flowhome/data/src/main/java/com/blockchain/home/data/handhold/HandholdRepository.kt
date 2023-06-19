package com.blockchain.home.data.handhold

import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResourceFlows
import com.blockchain.data.mapData
import com.blockchain.home.handhold.HandholStatus
import com.blockchain.home.handhold.HandholdService
import com.blockchain.home.handhold.HandholdStep
import com.blockchain.home.handhold.HandholdStepStatus
import com.blockchain.nabu.api.getuser.domain.UserService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class HandholdRepository(
    private val userService: UserService,
    private val kycService: KycService
) : HandholdService {
    override fun handholdTasksStatus(): Flow<DataResource<List<HandholdStepStatus>>> {
        val emailVerifiedStep = userService.getUserResourceFlow()
            .mapData { user ->
                HandholdStepStatus(
                    step = HandholdStep.VerifyEmail,
                    status = if (user.emailVerified) HandholStatus.Complete else HandholStatus.Incomplete
                )
            }

        val kycStep = kycService.stateFor(tierLevel = KycTier.GOLD)
            .mapData { goldState ->
                HandholdStepStatus(
                    step = HandholdStep.Kyc,
                    status = when (goldState) {
                        KycTierState.Verified -> HandholStatus.Complete

                        KycTierState.Pending,
                        KycTierState.UnderReview -> HandholStatus.Pending

                        KycTierState.Expired,
                        KycTierState.Rejected,
                        KycTierState.None -> HandholStatus.Incomplete
                    }
                )
            }

        val buyStep = flowOf(
            DataResource.Data(
                HandholdStepStatus(
                    step = HandholdStep.BuyCrypto, status = HandholStatus.Complete
                )
            )
        )

        return combineDataResourceFlows(
            emailVerifiedStep,
            kycStep,
            buyStep
        ) { emailVerifiedStatus, kycStatus, buyCryptoStatus ->
            listOf(emailVerifiedStatus, kycStatus, buyCryptoStatus)
        }
    }
}

private fun KycTierState.isVerified() = this == KycTierState.Verified
