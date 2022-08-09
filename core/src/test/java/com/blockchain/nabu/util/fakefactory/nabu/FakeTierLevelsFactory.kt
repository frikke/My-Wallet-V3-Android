package com.blockchain.nabu.util.fakefactory.nabu

import com.blockchain.api.kyc.model.KycLimitsDto
import com.blockchain.api.kyc.model.KycTierDto
import com.blockchain.api.kyc.model.KycTiersDto
import com.blockchain.nabu.models.responses.nabu.TierLevels

object FakeTierLevelsFactory {
    val any = TierLevels(0, 1, 2)
}

object FakeKycTiersFactory {
    val any = KycTiersDto(
        listOf(
            FakeTiersResponseFactory.tierZero,
            FakeTiersResponseFactory.tierOne,
            FakeTiersResponseFactory.tierTwo
        )
    )
}

object FakeTiersResponseFactory {
    val tierZero = KycTierDto(
        0,
        "Tier 0",
        state = "VERIFIED",
        limits = KycLimitsDto(
            currency = "USD",
            daily = null,
            annual = null
        )
    )

    val tierOne = KycTierDto(
        1,
        "Tier 1",
        state = "PENDING",
        limits = KycLimitsDto(
            currency = "USD",
            daily = null,
            annual = 1000.0.toBigDecimal()
        )
    )
    val tierTwo = KycTierDto(
        2,
        "Tier 2",
        state = "NONE",
        limits = KycLimitsDto(
            currency = "USD",
            daily = 25000.0.toBigDecimal(),
            annual = null
        )
    )
}
