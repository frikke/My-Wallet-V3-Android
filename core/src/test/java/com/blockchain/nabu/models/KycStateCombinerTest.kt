package com.blockchain.nabu.models

import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.Limits
import com.blockchain.nabu.models.responses.nabu.Tier
import com.blockchain.nabu.models.responses.nabu.Tiers
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
            assertTrue(it.isPendingFor(KycTierLevel.SILVER))
        }
        tiers(
            KycTierState.Verified,
            KycTierState.None
        ).let {
            assertTrue(it.isApprovedFor(KycTierLevel.SILVER))
        }
        tiers(
            KycTierState.Rejected,
            KycTierState.None
        ).let {
            assertTrue(it.isRejectedFor(KycTierLevel.SILVER))
        }
    }

    @Test
    fun `combinedState when tier 2 is Pending`() {
        tiers(
            KycTierState.None,
            KycTierState.Pending
        ).let {
            assertTrue(it.isPendingFor(KycTierLevel.GOLD))
        }

        tiers(
            KycTierState.Pending,
            KycTierState.Pending
        ).let {
            assertTrue(it.isPendingFor(KycTierLevel.GOLD))
        }

        tiers(
            KycTierState.Verified,
            KycTierState.Pending
        ).let {
            assertTrue(it.isPendingFor(KycTierLevel.GOLD))
        }

        tiers(
            KycTierState.Rejected,
            KycTierState.Pending
        ).let {
            assertTrue(it.isPendingFor(KycTierLevel.GOLD))
        }
    }

    @Test
    fun `combinedState when tier 2 is Approved`() {
        tiers(
            KycTierState.None,
            KycTierState.Verified
        ).let {
            assertTrue(it.isApprovedFor(KycTierLevel.GOLD))
        }
        tiers(
            KycTierState.Pending,
            KycTierState.Verified
        ).let {
            assertTrue(it.isApprovedFor(KycTierLevel.GOLD))
        }
        tiers(
            KycTierState.Verified,
            KycTierState.Verified
        ).let {
            assertTrue(it.isApprovedFor(KycTierLevel.GOLD))
        }
        tiers(
            KycTierState.Rejected,
            KycTierState.Verified
        ).let {
            assertTrue(it.isApprovedFor(KycTierLevel.GOLD))
        }
    }

    @Test
    fun `combinedState when tier 2 is Rejected`() {
        tiers(
            KycTierState.None,
            KycTierState.Rejected
        ).let {
            assertTrue(it.isRejectedFor(KycTierLevel.GOLD))
        }
        tiers(
            KycTierState.Pending,
            KycTierState.Rejected
        ).let {
            assertTrue(it.isRejectedFor(KycTierLevel.GOLD))
        }
        tiers(
            KycTierState.Verified,
            KycTierState.Rejected
        ).let {
            assertTrue(it.isRejectedFor(KycTierLevel.GOLD))
        }
        tiers(
            KycTierState.Rejected,
            KycTierState.Rejected
        ).let {
            assertTrue(it.isRejectedFor(KycTierLevel.GOLD))
        }
    }
}

private fun tiers(tier1State: KycTierState, tier2State: KycTierState): KycTiers {
    return KycTiers(
        Tiers(
            mapOf(
                KycTierLevel.BRONZE to
                    Tier(
                        KycTierState.Verified,
                        Limits(null, null)
                    ),
                KycTierLevel.SILVER to
                    Tier(
                        tier1State,
                        Limits(null, null)
                    ),
                KycTierLevel.GOLD to
                    Tier(
                        tier2State,
                        Limits(null, null)
                    )
            )
        )
    )
}
