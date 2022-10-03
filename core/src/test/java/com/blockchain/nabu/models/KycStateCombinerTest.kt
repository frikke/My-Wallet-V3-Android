package com.blockchain.nabu.models

import com.blockchain.core.kyc.domain.model.KycLimits
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierDetail
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.core.kyc.domain.model.TiersMap
import org.junit.Assert.assertTrue
import org.junit.Test

class KycStateCombinerTest {

    @Test
    fun `combinedState when tier 2 is None`() {
        tiers(
            KycTierState.None,
            KycTierState.None
        ).let {
            assertTrue(it.isInInitialState())
        }
        tiers(
            KycTierState.Pending,
            KycTierState.None
        ).let {
            assertTrue(it.isPendingFor(KycTier.SILVER))
        }
        tiers(
            KycTierState.Verified,
            KycTierState.None
        ).let {
            assertTrue(it.isApprovedFor(KycTier.SILVER))
        }
        tiers(
            KycTierState.Rejected,
            KycTierState.None
        ).let {
            assertTrue(it.isRejectedFor(KycTier.SILVER))
        }
    }

    @Test
    fun `combinedState when tier 2 is Pending`() {
        tiers(
            KycTierState.None,
            KycTierState.Pending
        ).let {
            assertTrue(it.isPendingFor(KycTier.GOLD))
        }

        tiers(
            KycTierState.Pending,
            KycTierState.Pending
        ).let {
            assertTrue(it.isPendingFor(KycTier.GOLD))
        }

        tiers(
            KycTierState.Verified,
            KycTierState.Pending
        ).let {
            assertTrue(it.isPendingFor(KycTier.GOLD))
        }

        tiers(
            KycTierState.Rejected,
            KycTierState.Pending
        ).let {
            assertTrue(it.isPendingFor(KycTier.GOLD))
        }
    }

    @Test
    fun `combinedState when tier 2 is Approved`() {
        tiers(
            KycTierState.None,
            KycTierState.Verified
        ).let {
            assertTrue(it.isApprovedFor(KycTier.GOLD))
        }
        tiers(
            KycTierState.Pending,
            KycTierState.Verified
        ).let {
            assertTrue(it.isApprovedFor(KycTier.GOLD))
        }
        tiers(
            KycTierState.Verified,
            KycTierState.Verified
        ).let {
            assertTrue(it.isApprovedFor(KycTier.GOLD))
        }
        tiers(
            KycTierState.Rejected,
            KycTierState.Verified
        ).let {
            assertTrue(it.isApprovedFor(KycTier.GOLD))
        }
    }

    @Test
    fun `combinedState when tier 2 is Rejected`() {
        tiers(
            KycTierState.None,
            KycTierState.Rejected
        ).let {
            assertTrue(it.isRejectedFor(KycTier.GOLD))
        }
        tiers(
            KycTierState.Pending,
            KycTierState.Rejected
        ).let {
            assertTrue(it.isRejectedFor(KycTier.GOLD))
        }
        tiers(
            KycTierState.Verified,
            KycTierState.Rejected
        ).let {
            assertTrue(it.isRejectedFor(KycTier.GOLD))
        }
        tiers(
            KycTierState.Rejected,
            KycTierState.Rejected
        ).let {
            assertTrue(it.isRejectedFor(KycTier.GOLD))
        }
    }
}

private fun tiers(tier1State: KycTierState, tier2State: KycTierState): KycTiers {
    return KycTiers(
        TiersMap(
            mapOf(
                KycTier.BRONZE to
                    KycTierDetail(
                        KycTierState.Verified,
                        KycLimits(null, null)
                    ),
                KycTier.SILVER to
                    KycTierDetail(
                        tier1State,
                        KycLimits(null, null)
                    ),
                KycTier.GOLD to
                    KycTierDetail(
                        tier2State,
                        KycLimits(null, null)
                    )
            )
        )
    )
}
