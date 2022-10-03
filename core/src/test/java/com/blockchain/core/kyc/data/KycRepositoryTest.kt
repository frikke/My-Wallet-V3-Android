package com.blockchain.core.kyc.data

import com.blockchain.api.kyc.model.KycLimitsDto
import com.blockchain.api.kyc.model.KycTierDto
import com.blockchain.api.kyc.model.KycTiersDto
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycLimits
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierDetail
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.core.kyc.domain.model.TiersMap
import com.blockchain.data.DataResource
import com.blockchain.nabu.USD
import com.blockchain.nabu.api.getuser.domain.UserService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Money
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class KycRepositoryTest {
    private val kycTiersStore = mockk<KycTiersStore>()
    private val userService = mockk<UserService>()
    private val assetCatalogue = mockk<AssetCatalogue>()

    private val kycService: KycService = KycRepository(
        kycTiersStore = kycTiersStore,
        userService = userService,
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
                KycTier.BRONZE to
                    KycTierDetail(
                        KycTierState.Verified,
                        KycLimits(null, null)
                    ),
                KycTier.SILVER to
                    KycTierDetail(
                        KycTierState.Pending,
                        KycLimits(null, Money.fromMajor(USD, 1000.0.toBigDecimal()))
                    ),
                KycTier.GOLD to
                    KycTierDetail(
                        KycTierState.None,
                        KycLimits(
                            Money.fromMajor(USD, 25000.0.toBigDecimal()), null
                        )
                    )
            )
        )
    )

    private fun createMockTiers(silverState: String, goldState: String): KycTiersDto {
        return KycTiersDto(
            listOf(
                KycTierDto(0, "BRONZE", "VERIFIED", null),
                KycTierDto(1, "SILVER", silverState, null),
                KycTierDto(2, "GOLD", goldState, null),
            )
        )
    }

    @Before
    fun setUp() {
        every { kycTiersStore.stream(any()) } returns
            flowOf(DataResource.Data(tiersResponse))

        every { assetCatalogue.fromNetworkTicker("USD") } returns USD
    }

    @Test
    fun `WHEN getKycTiers is called, THEN kycTiers should be returned`() {
        kycService.getTiersLegacy()
            .test()
            .await()
            .assertValue {
                it == kycTiers
            }
    }

    @Test
    fun `RX - GIVEN only BRONZE is verified, WHEN getHighestApprovedKycTierLegacy is called, BRONZE should be returned`() {
        val mockTiers = createMockTiers(silverState = "NONE", goldState = "NONE")
        every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

        kycService.getHighestApprovedTierLevelLegacy()
            .test()
            .await()
            .assertValue(KycTier.BRONZE)
    }

    @Test
    fun `GIVEN only BRONZE is verified, WHEN getHighestApprovedKycTierLegacy is called, BRONZE should be returned`() =
        runTest {
            val mockTiers = createMockTiers(silverState = "NONE", goldState = "NONE")
            every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

            val tierLevel = kycService.getHighestApprovedTierLevel().last()
            assertEquals(KycTier.BRONZE, tierLevel)
        }

    @Test
    fun `RX - GIVEN BRONZE and SILVER are verified, WHEN getHighestApprovedKycTierLegacy is called, SILVER should be returned`() {
        val mockTiers = createMockTiers(silverState = "VERIFIED", goldState = "NONE")
        every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

        kycService.getHighestApprovedTierLevelLegacy()
            .test()
            .await()
            .assertValue(KycTier.SILVER)
    }

    @Test
    fun `GIVEN BRONZE and SILVER are verified, WHEN getHighestApprovedKycTierLegacy is called, SILVER should be returned`() =
        runTest {
            val mockTiers = createMockTiers(silverState = "VERIFIED", goldState = "NONE")
            every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

            val tierLevel = kycService.getHighestApprovedTierLevel().last()
            assertEquals(KycTier.SILVER, tierLevel)
        }

    @Test
    fun `RX - GIVEN all are verified, WHEN getHighestApprovedKycTierLegacy is called, GOLD should be returned`() {
        val mockTiers = createMockTiers(silverState = "VERIFIED", goldState = "VERIFIED")
        every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

        kycService.getHighestApprovedTierLevelLegacy()
            .test()
            .await()
            .assertValue(KycTier.GOLD)
    }

    @Test
    fun `GIVEN all are verified, WHEN getHighestApprovedKycTierLegacy is called, GOLD should be returned`() =
        runTest {
            val mockTiers = createMockTiers(silverState = "VERIFIED", goldState = "VERIFIED")
            every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

            val tierLevel = kycService.getHighestApprovedTierLevel().last()
            assertEquals(KycTier.GOLD, tierLevel)
        }

    @Test
    fun `isKycRejected not pending success`() {
        val mockTiers = createMockTiers(silverState = "VERIFIED", goldState = "VERIFIED")
        every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

        kycService.isPendingFor(KycTier.GOLD)
            .test()
            .await()
            .assertValue(false)
    }

    @Test
    fun `isKycRejected pending success`() {
        val mockTiers = createMockTiers(silverState = "VERIFIED", goldState = "PENDING")
        every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

        kycService.isPendingFor(KycTier.GOLD)
            .test()
            .await()
            .assertValue(true)
    }

    @Test
    fun `isKycRejected not rejected success`() {
        val mockTiers = createMockTiers(silverState = "VERIFIED", goldState = "NONE")
        every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

        kycService.isRejected()
            .test()
            .await()
            .assertValue(false)
    }

    @Test
    fun `isKycRejected rejected success`() {
        val mockTiers = createMockTiers(silverState = "VERIFIED", goldState = "REJECTED")
        every { kycTiersStore.stream(any()) } returns flowOf(DataResource.Data(mockTiers))

        kycService.isRejected()
            .test()
            .await()
            .assertValue(true)
    }
}
