package com.blockchain.home.data.handhold

import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResourceFlows
import com.blockchain.data.mapData
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.home.handhold.HandholdService
import com.blockchain.home.handhold.HandholdStatus
import com.blockchain.home.handhold.HandholdTask
import com.blockchain.home.handhold.HandholdTasksStatus
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.preferences.HandholdPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class HandholdRepository(
    private val userService: UserService,
    private val kycService: KycService,
    private val tradingService: TradingService,
    private val interestService: InterestService,
    private val stakingService: StakingService,
    private val activeRewardsService: ActiveRewardsService,
    private val handholdPrefs: HandholdPrefs,
) : HandholdService {
    override fun handholdTasksStatus(): Flow<DataResource<List<HandholdTasksStatus>>> {
        if (handholdPrefs.overrideHandholdVerification) {
            return flowOf(
                DataResource.Data(
                    listOf(
                        HandholdTasksStatus(
                            task = HandholdTask.VerifyEmail,
                            status = if (handholdPrefs.debugHandholdEmailVerified) {
                                HandholdStatus.Complete
                            } else {
                                HandholdStatus.Incomplete
                            }
                        ),
                        HandholdTasksStatus(
                            task = HandholdTask.Kyc,
                            status = if (handholdPrefs.debugHandholdKycVerified) {
                                HandholdStatus.Complete
                            } else {
                                HandholdStatus.Incomplete
                            }
                        ),
                        HandholdTasksStatus(
                            task = HandholdTask.BuyCrypto,
                            status = if (handholdPrefs.debugHandholdBuyVerified) {
                                HandholdStatus.Complete
                            } else {
                                HandholdStatus.Incomplete
                            }
                        )
                    )
                )
            )
        } else {
            val emailVerifiedTask = userService.getUserResourceFlow()
                .mapData { user ->
                    HandholdTasksStatus(
                        task = HandholdTask.VerifyEmail,
                        status = if (user.emailVerified) HandholdStatus.Complete else HandholdStatus.Incomplete
                    )
                }

            val kycTask = kycService.stateFor(tierLevel = KycTier.GOLD)
                .mapData { goldState ->
                    HandholdTasksStatus(
                        task = HandholdTask.Kyc,
                        status = when (goldState) {
                            KycTierState.Verified -> HandholdStatus.Complete

                            KycTierState.Pending,
                            KycTierState.UnderReview -> HandholdStatus.Pending

                            KycTierState.Expired,
                            KycTierState.Rejected,
                            KycTierState.None -> HandholdStatus.Incomplete
                        }
                    )
                }

            // nabu-gateway/accounts/simplebuy for every currency
            // /savings for every currency
            // /staking for every currency
            // /earn_cc1w for every currency
            // https://blockc.slack.com/archives/C0551CRRYHM/p1686926159229149
            val buyTask = combine(
                tradingService.getActiveAssets().map { it.isNotEmpty() },
                interestService.getActiveAssets().map { it.isNotEmpty() },
                stakingService.getActiveAssets().map { it.isNotEmpty() },
                activeRewardsService.getActiveAssets().map { it.isNotEmpty() },
            ) { anyTradingAssets, anyInterestAssets, anyStakingAssets, anyActiveRewardsAssets ->
                anyTradingAssets || anyInterestAssets || anyStakingAssets || anyActiveRewardsAssets
            }.map { isBuyTaskComplete ->
                DataResource.Data(
                    HandholdTasksStatus(
                        task = HandholdTask.BuyCrypto,
                        status = if (isBuyTaskComplete) HandholdStatus.Complete else HandholdStatus.Incomplete
                    )
                )
            }

            return combineDataResourceFlows(
                emailVerifiedTask,
                kycTask,
                buyTask
            ) { emailVerifiedStatus, kycStatus, buyCryptoStatus ->
                listOf(emailVerifiedStatus, kycStatus, buyCryptoStatus)
            }.distinctUntilChanged()
        }
    }
}
