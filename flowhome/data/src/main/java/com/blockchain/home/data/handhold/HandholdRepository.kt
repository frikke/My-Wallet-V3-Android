package com.blockchain.home.data.handhold

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
    private val userService: UserService
) : HandholdService {
    override fun handholdTasksStatus(): Flow<DataResource<List<HandholdStepStatus>>> {
        val emailVerifiedStep = userService.getUserResourceFlow()
            .mapData { user ->
                HandholdStepStatus(
                    step = HandholdStep.VerifyEmail,
                    status = if (user.emailVerified) HandholStatus.Complete else HandholStatus.Incomplete
                )
            }

        val kycStep = flowOf(
            DataResource.Data(
                HandholdStepStatus(
                    step = HandholdStep.Kyc, status = HandholStatus.Pending
                )
            )
        )

        val buyStep = flowOf(
            DataResource.Data(
                HandholdStepStatus(
                    step = HandholdStep.BuyCrypto, status = HandholStatus.Incomplete
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
