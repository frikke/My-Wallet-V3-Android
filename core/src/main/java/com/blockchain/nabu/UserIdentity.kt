package com.blockchain.nabu

import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.domain.eligibility.model.TransactionsLimit
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.io.Serializable

interface UserIdentity {
    fun isEligibleFor(feature: Feature): Single<Boolean>
    fun isVerifiedFor(feature: Feature): Single<Boolean>
    fun getBasicProfileInformation(): Single<BasicProfileInfo>
    fun checkForUserWalletLinkErrors(): Completable
    fun getUserCountry(): Maybe<String>
    fun getUserState(): Maybe<String>
    fun userAccessForFeature(feature: Feature): Single<FeatureAccess>
    fun userAccessForFeatures(features: List<Feature>): Single<Map<Feature, FeatureAccess>>
    fun majorProductsNotEligibleReasons(): Single<List<ProductNotEligibleReason>>
    fun isArgentinian(): Single<Boolean>
    fun isCowboysUser(): Single<Boolean>
}

sealed class Feature {
    data class TierLevel(val tier: KycTier) : Feature()
    object SimplifiedDueDiligence : Feature()
    data class Interest(val currency: Currency) : Feature()
    object Buy : Feature()
    object Swap : Feature()
    object Sell : Feature()
    object DepositFiat : Feature()
    object DepositCrypto : Feature()
    object DepositInterest : Feature()
    object WithdrawFiat : Feature()
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

    fun isBlockedDueToEligibility(): Boolean =
        this is Blocked && reason is BlockedReason.NotEligible
}

sealed class BlockedReason : Serializable {
    data class NotEligible(val message: String?) : BlockedReason()
    sealed class InsufficientTier : BlockedReason() {
        object Tier1Required : InsufficientTier()
        object Tier2Required : InsufficientTier()
        object Tier1TradeLimitExceeded : InsufficientTier()
        data class Unknown(val message: String) : InsufficientTier()
    }
    sealed class Sanctions : BlockedReason() {
        object RussiaEU5 : Sanctions()
        data class Unknown(val message: String) : Sanctions()
    }
    class TooManyInFlightTransactions(val maxTransactions: Int) : BlockedReason()
}
