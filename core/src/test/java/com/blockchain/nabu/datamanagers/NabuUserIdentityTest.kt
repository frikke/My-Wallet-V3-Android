package com.blockchain.nabu.datamanagers

import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProvider
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.Limits
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.Tiers
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
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
    private val eligibilityService: EligibilityService = mock()
    private val nabuToken: NabuToken = mock()
    private val nabuDataManager: NabuDataManager = mock()

    private val subject = NabuUserIdentity(
        custodialWalletManager = custodialWalletManager,
        interestEligibilityProvider = interestEligibilityProvider,
        simpleBuyEligibilityProvider = simpleBuyEligibilityProvider,
        nabuUserDataManager = nabuUserDataManager,
        nabuDataProvider = nabuDataProvider,
        eligibilityService = eligibilityService,
        nabuToken = nabuToken,
        nabu = nabuDataManager
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

    @Test
    fun `on userAccessForFeature Buy should query eligibility data manager`() {
        val eligibility = ProductEligibility(
            product = EligibleProduct.BUY,
            canTransact = true,
            maxTransactionsCap = TransactionsLimit.Unlimited,
            canUpgradeTier = false
        )
        whenever(eligibilityService.getProductEligibility(EligibleProduct.BUY))
            .thenReturn(Single.just(eligibility))

        subject.userAccessForFeature(Feature.Buy)
            .test()
            .assertValue(FeatureAccess.Granted())
    }

    @Test
    fun `on userAccessForFeature Swap should query eligibility data manager`() {
        val transactionsLimit = TransactionsLimit.Limited(3, 1)
        val eligibility = ProductEligibility(
            product = EligibleProduct.SWAP,
            canTransact = true,
            maxTransactionsCap = transactionsLimit,
            canUpgradeTier = false
        )
        whenever(eligibilityService.getProductEligibility(EligibleProduct.SWAP))
            .thenReturn(Single.just(eligibility))

        subject.userAccessForFeature(Feature.Swap)
            .test()
            .assertValue(FeatureAccess.Granted(transactionsLimit))
    }

    @Test
    fun `on userAccessForFeature CryptoDeposit should query eligibility data manager`() {
        val eligibility = ProductEligibility(
            product = EligibleProduct.CRYPTO_DEPOSIT,
            canTransact = false,
            maxTransactionsCap = TransactionsLimit.Unlimited,
            canUpgradeTier = true
        )
        whenever(eligibilityService.getProductEligibility(EligibleProduct.CRYPTO_DEPOSIT))
            .thenReturn(Single.just(eligibility))

        subject.userAccessForFeature(Feature.CryptoDeposit)
            .test()
            .assertValue(FeatureAccess.Blocked(BlockedReason.InsufficientTier))
    }

    @Test
    fun `has received STX airdrop`() {
        val userId = "user_id"
        val userToken = "user_token"
        val mockOfflineTokenResponse = NabuOfflineTokenResponse(userId, userToken)
        val mockNabuUser: NabuUser = mock {
            on { isStxAirdropRegistered }.thenReturn(true)
        }
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(mockOfflineTokenResponse))
        whenever(nabuDataManager.getUser(mockOfflineTokenResponse)).thenReturn(Single.just(mockNabuUser))

        subject.hasReceivedStxAirdrop()
            .test()
            .assertValue(true)
    }

    @Test
    fun `has not received STX airdrop`() {
        val userId = "user_id"
        val userToken = "user_token"
        val mockOfflineTokenResponse = NabuOfflineTokenResponse(userId, userToken)
        val mockNabuUser: NabuUser = mock {
            on { isStxAirdropRegistered }.thenReturn(false)
        }
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(mockOfflineTokenResponse))
        whenever(nabuDataManager.getUser(mockOfflineTokenResponse)).thenReturn(Single.just(mockNabuUser))

        subject.hasReceivedStxAirdrop()
            .test()
            .assertValue(false)
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
