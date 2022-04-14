package com.blockchain.nabu.datamanagers

import com.blockchain.core.eligibility.EligibilityDataManager
import com.blockchain.core.eligibility.models.EligibleProduct
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.repositories.interest.InterestEligibilityProvider
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibility
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith

class NabuUserIdentity(
    private val custodialWalletManager: CustodialWalletManager,
    private val interestEligibilityProvider: InterestEligibilityProvider,
    private val simpleBuyEligibilityProvider: SimpleBuyEligibilityProvider,
    private val nabuUserDataManager: NabuUserDataManager,
    private val nabuDataProvider: NabuDataUserProvider,
    private val eligibilityDataManager: EligibilityDataManager
) : UserIdentity {
    override fun isEligibleFor(feature: Feature): Single<Boolean> {
        return when (feature) {
            is Feature.TierLevel -> nabuUserDataManager.tiers().map {
                it.isNotInitialisedFor(feature.tier.toKycTierLevel())
            }
            is Feature.SimpleBuy -> simpleBuyEligibilityProvider.isEligibleForSimpleBuy()
            is Feature.CustodialAccounts -> Single.zip(
                nabuDataProvider.getUser(),
                simpleBuyEligibilityProvider.simpleBuyTradingEligibility()
            ) { nabuUser, sbEligibility -> nabuUser.currentTier == Tier.GOLD.ordinal && sbEligibility.eligible }
            is Feature.Interest -> interestEligibilityProvider.getEligibilityForCustodialAssets()
                .map { assets -> assets.map { it.cryptoCurrency }.contains(feature.currency) }
            is Feature.SimplifiedDueDiligence -> custodialWalletManager.isSimplifiedDueDiligenceEligible()
            Feature.Buy -> userAccessForFeature(feature).map { it is FeatureAccess.Granted }
            Feature.CryptoDeposit -> userAccessForFeature(feature).map { it is FeatureAccess.Granted }
            Feature.Swap -> userAccessForFeature(feature).map { it is FeatureAccess.Granted }
        }
    }

    override fun isVerifiedFor(feature: Feature): Single<Boolean> {
        return when (feature) {
            is Feature.TierLevel -> nabuUserDataManager.tiers().map {
                it.isApprovedFor(feature.tier.toKycTierLevel())
            }
            is Feature.SimplifiedDueDiligence -> custodialWalletManager.fetchSimplifiedDueDiligenceUserState().map {
                it.isVerified
            }
            is Feature.CustodialAccounts -> Single.zip(
                nabuDataProvider.getUser(),
                simpleBuyEligibilityProvider.simpleBuyTradingEligibility()
            ) { nabuUser, sbEligibility -> nabuUser.currentTier == Tier.GOLD.ordinal && sbEligibility.eligible }
            is Feature.SimpleBuy,
            is Feature.Interest,
            Feature.Buy,
            Feature.CryptoDeposit,
            Feature.Swap -> throw IllegalArgumentException("Cannot be verified for $feature")
        }.exhaustive
    }

    override fun getHighestApprovedKycTier(): Single<Tier> = nabuUserDataManager.tiers().map { tiers ->
        val approvedTier = KycTierLevel.values().reversed().find {
            tiers.isApprovedFor(it)
        }
        approvedTier?.toTier() ?: throw IllegalStateException("No approved tiers")
    }

    override fun isKycPending(tier: Tier): Single<Boolean> = nabuUserDataManager.tiers().map { tiers ->
        tiers.isPendingOrUnderReviewFor(tier.toKycTierLevel())
    }

    override fun isKycRejected(): Single<Boolean> = nabuUserDataManager.tiers().map { tiers ->
        KycTierLevel.values().any { level ->
            tiers.isRejectedFor(level)
        }
    }

    override fun isRejectedForTier(feature: Feature.TierLevel): Single<Boolean> {
        return nabuUserDataManager.tiers().map {
            it.isRejectedFor(feature.tier.toKycTierLevel())
        }
    }

    override fun isKycInProgress(): Single<Boolean> =
        nabuDataProvider
            .getUser()
            .map { it.tiers?.next ?: 0 }
            .zipWith(nabuUserDataManager.tiers())
            .map { (user, tiers) ->
                tiers.isNotInitialisedFor(KycTierLevel.values()[user])
            }

    override fun getBasicProfileInformation(): Single<BasicProfileInfo> =
        nabuDataProvider.getUser().map {
            it.toBasicProfileInfo()
        }

    override fun getUserCountry(): Maybe<String> =
        nabuDataProvider.getUser().flatMapMaybe {
            val countryCode = it.address?.countryCode
            if (countryCode.isNullOrEmpty()) {
                Maybe.empty()
            } else {
                Maybe.just(countryCode)
            }
        }

    override fun getUserState(): Maybe<String> =
        nabuDataProvider.getUser().flatMapMaybe {
            val state = it.address?.state
            if (state.isNullOrEmpty()) {
                Maybe.empty()
            } else {
                Maybe.just(state)
            }
        }

    override fun userAccessForFeatures(features: List<Feature>): Single<List<Pair<Feature, FeatureAccess>>> {
        return features.map { feature ->
            userAccessForFeature(feature).map { access ->
                Pair(feature, access)
            }
        }.zipSingles()
    }

    // converts a List<Single<Items>> -> Single<List<Items>>
    private fun <T> List<Single<T>>.zipSingles(): Single<List<T>> {
        if (this.isEmpty()) return Single.just(emptyList())
        return Single.zip(this) {
            @Suppress("UNCHECKED_CAST")
            return@zip (it as Array<T>).toList()
        }
    }

    override fun userAccessForFeature(feature: Feature): Single<FeatureAccess> {
        return when (feature) {
            Feature.SimpleBuy -> simpleBuyEligibilityProvider.simpleBuyTradingEligibility().zipWith(
                isVerifiedFor(Feature.TierLevel(Tier.GOLD))
            ).map { (eligibility, isGold) ->
                simpleBuyAccessState(eligibility, isGold)
            }
            Feature.CustodialAccounts ->
                Single.zip(
                    nabuDataProvider.getUser(),
                    simpleBuyEligibilityProvider.simpleBuyTradingEligibility()
                ) { nabuUser, sbEligibility ->
                    when {
                        nabuUser.currentTier == Tier.GOLD.ordinal && sbEligibility.eligible -> {
                            FeatureAccess.Granted()
                        }
                        nabuUser.currentTier != Tier.GOLD.ordinal -> {
                            FeatureAccess.NotRequested
                        }
                        else -> {
                            FeatureAccess.Blocked(BlockedReason.NotEligible)
                        }
                    }
                }
            Feature.Buy -> eligibilityDataManager.getProductEligibility(EligibleProduct.BUY).map { eligibility ->
                if (eligibility.canTransact) FeatureAccess.Granted(eligibility.maxTransactionsCap)
                else FeatureAccess.Blocked(
                    if (eligibility.canUpgradeTier) BlockedReason.InsufficientTier
                    else BlockedReason.NotEligible
                )
            }
            Feature.Swap -> eligibilityDataManager.getProductEligibility(EligibleProduct.SWAP).map { eligibility ->
                if (eligibility.canTransact) FeatureAccess.Granted(eligibility.maxTransactionsCap)
                else FeatureAccess.Blocked(
                    if (eligibility.canUpgradeTier) BlockedReason.InsufficientTier
                    else BlockedReason.NotEligible
                )
            }
            Feature.CryptoDeposit -> eligibilityDataManager.getProductEligibility(EligibleProduct.CRYPTO_DEPOSIT)
                .map { eligibility ->
                    if (eligibility.canTransact) FeatureAccess.Granted(eligibility.maxTransactionsCap)
                    else FeatureAccess.Blocked(
                        if (eligibility.canUpgradeTier) BlockedReason.InsufficientTier
                        else BlockedReason.NotEligible
                    )
                }
            is Feature.Interest,
            Feature.SimplifiedDueDiligence,
            is Feature.TierLevel -> TODO("Not Implemented Yet")
        }
    }

    private fun simpleBuyAccessState(eligibility: SimpleBuyEligibility, gold: Boolean): FeatureAccess {
        return when {
            !eligibility.simpleBuyTradingEligible && gold -> FeatureAccess.Blocked(BlockedReason.NotEligible)
            !eligibility.simpleBuyTradingEligible -> FeatureAccess.NotRequested
            eligibility.canCreateOrder() -> FeatureAccess.Granted()
            else -> FeatureAccess.Blocked(
                BlockedReason.TooManyInFlightTransactions(
                    eligibility.maxPendingDepositSimpleBuyTrades
                )
            )
        }
    }

    private fun NabuUser.toBasicProfileInfo() =
        BasicProfileInfo(
            firstName = firstName ?: email,
            lastName = lastName ?: email,
            email = email
        )

    override fun isKycResubmissionRequired(): Single<Boolean> =
        nabuDataProvider.getUser().map { it.isMarkedForResubmission }

    override fun shouldResubmitAfterRecovery(): Single<Boolean> =
        nabuDataProvider.getUser().map { it.isMarkedForRecoveryResubmission }

    override fun checkForUserWalletLinkErrors(): Completable =
        nabuDataProvider.getUser().flatMapCompletable { Completable.complete() }

    private fun Tier.toKycTierLevel(): KycTierLevel =
        when (this) {
            Tier.BRONZE -> KycTierLevel.BRONZE
            Tier.SILVER -> KycTierLevel.SILVER
            Tier.GOLD -> KycTierLevel.GOLD
        }

    private fun KycTierLevel.toTier(): Tier =
        when (this) {
            KycTierLevel.BRONZE -> Tier.BRONZE
            KycTierLevel.SILVER -> Tier.SILVER
            KycTierLevel.GOLD -> Tier.GOLD
        }
}

private fun SimpleBuyEligibility.canCreateOrder(): Boolean =
    pendingDepositSimpleBuyTrades < maxPendingDepositSimpleBuyTrades
