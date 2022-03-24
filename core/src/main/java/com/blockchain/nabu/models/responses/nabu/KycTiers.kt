package com.blockchain.nabu.models.responses.nabu

import com.blockchain.serialization.JsonSerializable
import info.blockchain.balance.Money
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class KycTiers(
    private val tiers: Tiers
) : JsonSerializable {

    fun isApprovedFor(level: KycTierLevel) = tiers[level].state == KycTierState.Verified
    fun isPendingFor(level: KycTierLevel) = tiers[level].state == KycTierState.Pending
    fun isUnderReviewFor(level: KycTierLevel) = tiers[level].state == KycTierState.UnderReview
    fun isPendingOrUnderReviewFor(level: KycTierLevel) = isUnderReviewFor(level) || isPendingFor(level)
    fun isRejectedFor(level: KycTierLevel) = tiers[level].state == KycTierState.Rejected
    fun isNotInitialisedFor(level: KycTierLevel) = tiers[level].state == KycTierState.None
    fun isInitialisedFor(level: KycTierLevel) = tiers[level].state != KycTierState.None
    fun isInitialised() = tiers[KycTierLevel.BRONZE].state != KycTierState.None
    fun isInInitialState() = tiers[KycTierLevel.SILVER].state == KycTierState.None
    fun tierForIndex(index: Int) = tiers[KycTierLevel.values()[index]]
    fun tierForLevel(level: KycTierLevel) = tiers[level]
    fun tierCompletedForLevel(level: KycTierLevel) =
        isApprovedFor(level) || isRejectedFor(level) || isPendingFor(level)

    fun highestActiveLevelState(): KycTierState =
        tiers.entries.reversed().firstOrNull {
            it.value.state != KycTierState.None
        }?.value?.state ?: KycTierState.None

    fun isVerified() = isApprovedFor(KycTierLevel.SILVER) || isApprovedFor(KycTierLevel.GOLD)

    companion object {
        fun default() =
            KycTiers(
                Tiers(
                    KycTierLevel.values().map {
                        it to Tier(KycTierState.None, null)
                    }.toMap()
                )
            )
    }
}

@Serializable
data class Limits(
    val dailyLimit: @Contextual Money? = null,
    val annualLimit: @Contextual Money? = null
)

@Serializable
data class Tier(
    val state: KycTierState,
    val limits: Limits? = null
)

@Serializable(with = KycTierStateAdapter.KycTierStateSerializer::class)
enum class KycTierState {
    None,
    Rejected,
    Pending,
    Verified,
    UnderReview,
    Expired
}

@Serializable
enum class KycTierLevel {
    BRONZE,
    SILVER,
    GOLD
}

@Serializable
data class Tiers(
    private val map: Map<KycTierLevel, Tier>
) : Map<KycTierLevel, Tier> by map {
    override operator fun get(key: KycTierLevel): Tier {
        return map.getOrElse(key) {
            throw IllegalArgumentException("$key is not a known KycTierLevel")
        }
    }
}
