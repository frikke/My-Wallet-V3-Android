package com.blockchain.nabu.datamanagers

import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProvider
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.Limits
import com.blockchain.nabu.models.responses.nabu.Tiers
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Test

class NabuUserIdentityTest {

    private val custodialWalletManager: CustodialWalletManager = mock()
    private val interestEligibilityProvider: InterestEligibilityProvider = mock()
    private val simpleBuyEligibilityProvider: SimpleBuyEligibilityProvider = mock()
    private val nabuUserDataManager: NabuUserDataManager = mock()
    private val nabuDataProvider: NabuDataUserProvider = mock()

    private val subject = NabuUserIdentity(
        custodialWalletManager = custodialWalletManager,
        interestEligibilityProvider = interestEligibilityProvider,
        simpleBuyEligibilityProvider = simpleBuyEligibilityProvider,
        nabuUserDataManager = nabuUserDataManager,
        nabuDataProvider = nabuDataProvider,
    )

    @Test
    fun `getHighestApprovedKycTier bronze success`() {
        val mockTiers = createMockTiers(tier1 = KycTierState.None, tier2 = KycTierState.None)
        whenever(nabuUserDataManager.tiers()).thenReturn(Single.just(mockTiers))

        subject.getHighestApprovedKycTier()
            .test()
            .assertValue(Tier.BRONZE)

        verify(nabuUserDataManager).tiers()
    }

    @Test
    fun `getHighestApprovedKycTier silver success`() {
        val mockTiers = createMockTiers(tier1 = KycTierState.Verified, tier2 = KycTierState.Rejected)
        whenever(nabuUserDataManager.tiers()).thenReturn(Single.just(mockTiers))

        subject.getHighestApprovedKycTier()
            .test()
            .assertValue(Tier.SILVER)

        verify(nabuUserDataManager).tiers()
    }

    @Test
    fun `getHighestApprovedKycTier gold success`() {
        val mockTiers = createMockTiers(tier1 = KycTierState.Verified, tier2 = KycTierState.Verified)
        whenever(nabuUserDataManager.tiers()).thenReturn(Single.just(mockTiers))

        subject.getHighestApprovedKycTier()
            .test()
            .assertValue(Tier.GOLD)

        verify(nabuUserDataManager).tiers()
    }

    @Test
    fun `isKycRejected not rejected success`() {
        val mockTiers = createMockTiers(tier1 = KycTierState.Verified, tier2 = KycTierState.None)
        whenever(nabuUserDataManager.tiers()).thenReturn(Single.just(mockTiers))

        subject.isKycRejected()
            .test()
            .assertValue(false)
    }

    @Test
    fun `isKycRejected rejected success`() {
        val mockTiers = createMockTiers(tier1 = KycTierState.Verified, tier2 = KycTierState.Rejected)
        whenever(nabuUserDataManager.tiers()).thenReturn(Single.just(mockTiers))

        subject.isKycRejected()
            .test()
            .assertValue(true)
    }

    @Test
    fun `isKycRejected not pending success`() {
        val mockTiers = createMockTiers(tier1 = KycTierState.Verified, tier2 = KycTierState.None)
        whenever(nabuUserDataManager.tiers()).thenReturn(Single.just(mockTiers))

        subject.isKycPending(Tier.GOLD)
            .test()
            .assertValue(false)
    }

    @Test
    fun `isKycRejected pending success`() {
        val mockTiers = createMockTiers(tier1 = KycTierState.Verified, tier2 = KycTierState.Pending)
        whenever(nabuUserDataManager.tiers()).thenReturn(Single.just(mockTiers))

        subject.isKycPending(Tier.GOLD)
            .test()
            .assertValue(true)
    }

    companion object {
        fun createMockTiers(tier1: KycTierState, tier2: KycTierState): KycTiers {
            return KycTiers(
                Tiers(
                    mapOf(
                        KycTierLevel.BRONZE to
                            com.blockchain.nabu.models.responses.nabu.Tier(
                                KycTierState.Verified,
                                Limits(null, null)
                            ),
                        KycTierLevel.SILVER to
                            com.blockchain.nabu.models.responses.nabu.Tier(
                                tier1,
                                Limits(null, null)
                            ),
                        KycTierLevel.GOLD to
                            com.blockchain.nabu.models.responses.nabu.Tier(
                                tier2,
                                Limits(null, null)
                            )
                    )
                )
            )
        }
    }
}
