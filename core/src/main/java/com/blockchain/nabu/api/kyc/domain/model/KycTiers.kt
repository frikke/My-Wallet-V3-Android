package com.blockchain.nabu.api.kyc.domain.model

import info.blockchain.balance.Money

// todo(othman) keep track - removed serializable,
// if serialization is found to be needed, get data from cache instead
data class KycTiers(
    private val tiersMap: TiersMap
) {
    fun isApprovedFor(level: KycTierLevel) = tiersMap[level].state == KycTierState.Verified
    fun isPendingFor(level: KycTierLevel) = tiersMap[level].state == KycTierState.Pending
    fun isUnderReviewFor(level: KycTierLevel) = tiersMap[level].state == KycTierState.UnderReview
    fun isPendingOrUnderReviewFor(level: KycTierLevel) = isUnderReviewFor(level) || isPendingFor(level)
    fun isRejectedFor(level: KycTierLevel) = tiersMap[level].state == KycTierState.Rejected
    fun isNotInitialisedFor(level: KycTierLevel) = tiersMap[level].state == KycTierState.None
    fun isInitialisedFor(level: KycTierLevel) = tiersMap[level].state != KycTierState.None
    fun isInitialised() = tiersMap[KycTierLevel.BRONZE].state != KycTierState.None
    fun isInInitialState() = tiersMap[KycTierLevel.SILVER].state == KycTierState.None
    fun tierForLevel(level: KycTierLevel) = tiersMap[level]
    fun tierCompletedForLevel(level: KycTierLevel) =
        isApprovedFor(level) || isRejectedFor(level) || isPendingFor(level)

    fun highestActiveLevelState(): KycTierState =
        tiersMap.entries.reversed().firstOrNull {
            it.value.state != KycTierState.None
        }?.value?.state ?: KycTierState.None

    fun isVerified() = isApprovedFor(KycTierLevel.SILVER) || isApprovedFor(KycTierLevel.GOLD)

    companion object {
        fun default() =
            KycTiers(
                TiersMap(
                    KycTierLevel.values().map {
                        it to KycTierDetail(KycTierState.None, null)
                    }.toMap()
                )
            )
    }
}

enum class KycTierState {
    None,
    Rejected,
    Pending,
    Verified,
    UnderReview,
    Expired;

    companion object {
        fun fromValue(value: String): KycTierState {
            return when (value.uppercase()) {
                "NONE" -> None
                "REJECTED" -> Rejected
                "PENDING" -> Pending
                "VERIFIED" -> Verified
                "UNDER_REVIEW" -> UnderReview
                "EXPIRED" -> Expired
                else -> throw Exception("Unknown KYC Tier State: $value, unsupported data type")
            }
        }
    }
}

data class KycLimits(
    val dailyLimit: Money? = null,
    val annualLimit: Money? = null
)

data class KycTierDetail(
    val state: KycTierState,
    val kycLimits: KycLimits? = null
)

enum class KycTierLevel {
    BRONZE,
    SILVER,
    GOLD
}

/**
 * Holds each [KycTierLevel] with its corresponding [KycTierDetail],
 * which defines the [KycTierState] (pending, approved etc), and the [KycLimits] (daily, annual limits)
 */
data class TiersMap(
    private val map: Map<KycTierLevel, KycTierDetail>
) : Map<KycTierLevel, KycTierDetail> by map {
    override operator fun get(key: KycTierLevel): KycTierDetail {
        return map.getOrElse(key) {
            throw IllegalArgumentException("$key is not a known KycTierLevel")
        }
    }
}
