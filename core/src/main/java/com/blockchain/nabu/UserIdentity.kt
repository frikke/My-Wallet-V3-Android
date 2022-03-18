package com.blockchain.nabu

import com.blockchain.core.eligibility.models.TransactionsLimit
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.io.Serializable

interface UserIdentity {
    fun isEligibleFor(feature: Feature): Single<Boolean>
    fun isVerifiedFor(feature: Feature): Single<Boolean>
    fun getHighestApprovedKycTier(): Single<Tier>
    fun isKycPending(tier: Tier): Single<Boolean>
    fun isKycRejected(): Single<Boolean>
    fun isKycInProgress(): Single<Boolean>
    fun isRejectedForTier(feature: Feature.TierLevel): Single<Boolean>
    fun isKycResubmissionRequired(): Single<Boolean>
    fun shouldResubmitAfterRecovery(): Single<Boolean>
    fun getBasicProfileInformation(): Single<BasicProfileInfo>
    fun checkForUserWalletLinkErrors(): Completable
    fun getUserCountry(): Maybe<String>
    fun getUserState(): Maybe<String>
    fun userAccessForFeature(feature: Feature): Single<FeatureAccess>
}

sealed class Feature {
    data class TierLevel(val tier: Tier) : Feature()
    object SimplifiedDueDiligence : Feature()
    data class Interest(val currency: Currency) : Feature()
    object SimpleBuy : Feature()
    object CustodialAccounts : Feature()
    object Buy : Feature()
    object Swap : Feature()
    object CryptoDeposit : Feature()
}

/**
 in ordinal order:
 0 - no kyc
 1 - email & address verified
 2 - identity documents verified
 3 - simplified due diligence eligible; user with tier 1 verification in specific low risk countries
 */
enum class Tier {
    BRONZE, SILVER, GOLD
}

data class BasicProfileInfo(
    val firstName: String,
    val lastName: String,
    val email: String
) : Serializable

sealed class FeatureAccess {
    data class Granted(
        // Only used by Feature.Buy and Feature.Swap
        val transactionsLimit: TransactionsLimit = TransactionsLimit.Unlimited
    ) : FeatureAccess()
    data class Blocked(val reason: BlockedReason) : FeatureAccess()
    object NotRequested : FeatureAccess()
    object Unknown : FeatureAccess() // Used mostly for initialisation purposes

    fun isBlockedDueToEligibility(): Boolean =
        this is Blocked && reason == BlockedReason.NotEligible
}

sealed class BlockedReason {
    object NotEligible : BlockedReason()
    object InsufficientTier : BlockedReason()
    class TooManyInFlightTransactions(val maxTransactions: Int) : BlockedReason()
}
