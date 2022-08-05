package com.blockchain.nabu.api.kyc.data

import com.blockchain.api.kyc.model.KycLimitsDto
import com.blockchain.api.kyc.model.KycTierDto
import com.blockchain.api.kyc.model.KycTiersDto
import com.blockchain.data.DataResource
import com.blockchain.nabu.USD
import com.blockchain.nabu.api.kyc.data.store.KycTiersStore
import com.blockchain.nabu.api.kyc.domain.KycService
import com.blockchain.nabu.api.kyc.domain.model.KycLimits
import com.blockchain.nabu.api.kyc.domain.model.KycTierDetail
import com.blockchain.nabu.api.kyc.domain.model.KycTierLevel
import com.blockchain.nabu.api.kyc.domain.model.KycTierState
import com.blockchain.nabu.api.kyc.domain.model.KycTiers
import com.blockchain.nabu.api.kyc.domain.model.TiersMap
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Money
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

class KycTiersStoreRepositoryTest {
    private val kycTiersStore = mockk<KycTiersStore>()
    private val assetCatalogue = mockk<AssetCatalogue>()

    private val kycService: KycService = KycRepository(
        kycTiersStore = kycTiersStore,
        assetCatalogue = assetCatalogue
    )

    private val tiersResponse = KycTiersDto(
        listOf(
            KycTierDto(
                index = 0,
                name = "name",
                state = "VERIFIED",
                limits = null
            ),
            KycTierDto(
                index = 1,
                name = "name",
                state = "PENDING",
                limits = KycLimitsDto(
                    currency = "USD",
                    daily = null,
                    annual = 1000.0.toBigDecimal()
                )
            ),
            KycTierDto(
                index = 2,
                name = "name",
                state = "NONE",
                limits = KycLimitsDto(
                    currency = "USD",
                    daily = 25000.0.toBigDecimal(),
                    annual = null
                )
            )
        )
    )

    private val kycTiers = KycTiers(
        TiersMap(
            mapOf(
                KycTierLevel.BRONZE to
                    KycTierDetail(
                        KycTierState.Verified,
                        KycLimits(null, null)
                    ),
                KycTierLevel.SILVER to
                    KycTierDetail(
                        KycTierState.Pending,
                        KycLimits(null, Money.fromMajor(USD, 1000.0.toBigDecimal()))
                    ),
                KycTierLevel.GOLD to
                    KycTierDetail(
                        KycTierState.None,
                        KycLimits(
                            Money.fromMajor(USD, 25000.0.toBigDecimal()), null
                        )
                    )
            )
        )
    )

    @Before
    fun setUp() {
        every { kycTiersStore.stream(any()) } returns
            flowOf(DataResource.Data(tiersResponse))

        every { assetCatalogue.fromNetworkTicker("USD") } returns USD
    }

    @Test
    fun `WHEN getKycTiers is called, THEN kycTiers should be returned`() {
        kycService.getKycTiersLegacy()
            .test()
            .await()
            .assertValue {
                it == kycTiers
            }
    }
}
