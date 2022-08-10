package com.blockchain.nabu.datamanagers

import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.extensions.exhaustive
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.api.kyc.domain.model.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome
import piuk.blockchain.androidcore.utils.extensions.zipSingles

class NabuUserIdentity(
    private val custodialWalletManager: CustodialWalletManager,
    private val interestService: InterestService,
    private val simpleBuyEligibilityProvider: SimpleBuyEligibilityProvider,
    private val nabuUserDataManager: NabuUserDataManager,
    private val userService: UserService,
    private val eligibilityService: EligibilityService,
    private val bindFeatureFlag: FeatureFlag
) : UserIdentity {
    override fun isEligibleFor(feature: Feature): Single<Boolean> {
        return when (feature) {
            is Feature.TierLevel -> nabuUserDataManager.tiers().map {
                it.isNotInitialisedFor(feature.tier.toKycTierLevel())
            }
            is Feature.Interest -> interestService.getEligibilityForAssets()
                .map { mapAssetWithEligibility -> mapAssetWithEligibility.containsKey(feature.currency) }
            is Feature.SimplifiedDueDiligence -> custodialWalletManager.isSimplifiedDueDiligenceEligible()
            Feature.Buy,
            Feature.Swap,
            Feature.Sell,
            Feature.DepositCrypto,
            Feature.DepositFiat,
            Feature.DepositInterest,
            Feature.WithdrawFiat -> userAccessForFeature(feature).map { it is FeatureAccess.Granted }
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
            is Feature.Interest,
            Feature.Buy,
            Feature.DepositCrypto,
            Feature.Swap,
            Feature.DepositFiat,
            Feature.DepositInterest,
            Feature.Sell,
            Feature.WithdrawFiat -> throw IllegalArgumentException("Cannot be verified for $feature")
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
        userService
            .getUser()
            .map { it.tiers?.next ?: 0 }
            .zipWith(nabuUserDataManager.tiers())
            .map { (user, tiers) ->
                tiers.isNotInitialisedFor(KycTierLevel.values()[user])
            }

    override fun getBasicProfileInformation(): Single<BasicProfileInfo> =
        userService.getUser().map {
            it.toBasicProfileInfo()
        }

    override fun getUserCountry(): Maybe<String> =
        userService.getUser().flatMapMaybe {
            val countryCode = it.address?.countryCode
            if (countryCode.isNullOrEmpty()) {
                Maybe.empty()
            } else {
                Maybe.just(countryCode)
            }
        }

    override fun getUserState(): Maybe<String> =
        userService.getUser().flatMapMaybe {
            val state = it.address?.state
            if (state.isNullOrEmpty()) {
                Maybe.empty()
            } else {
                Maybe.just(state)
            }
        }

    override fun userAccessForFeatures(features: List<Feature>): Single<Map<Feature, FeatureAccess>> =
        features.map { feature ->
            userAccessForFeature(feature).map { access ->
                Pair(feature, access)
            }
        }.zipSingles()
            .map { mapOf(*it.toTypedArray()) }

    override fun userAccessForFeature(feature: Feature): Single<FeatureAccess> {
        return when (feature) {
            Feature.Buy ->
                Single.zip(
                    rxSingleOutcome { eligibilityService.getProductEligibility(EligibleProduct.BUY) },
                    simpleBuyEligibilityProvider.simpleBuyTradingEligibility()
                ) { buyEligibility, sbEligibility ->
                    val buyFeatureAccess = buyEligibility.toFeatureAccess()

                    when {
                        buyFeatureAccess !is FeatureAccess.Granted -> buyFeatureAccess
                        sbEligibility.pendingDepositSimpleBuyTrades < sbEligibility.maxPendingDepositSimpleBuyTrades ->
                            FeatureAccess.Granted()
                        else -> FeatureAccess.Blocked(
                            BlockedReason.TooManyInFlightTransactions(
                                sbEligibility.maxPendingDepositSimpleBuyTrades
                            )
                        )
                    }
                }
            Feature.Swap ->
                rxSingleOutcome { eligibilityService.getProductEligibility(EligibleProduct.SWAP) }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.Sell ->
                rxSingleOutcome { eligibilityService.getProductEligibility(EligibleProduct.SELL) }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.DepositFiat ->
                rxSingleOutcome { eligibilityService.getProductEligibility(EligibleProduct.DEPOSIT_FIAT) }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.DepositCrypto ->
                rxSingleOutcome { eligibilityService.getProductEligibility(EligibleProduct.DEPOSIT_CRYPTO) }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.DepositInterest ->
                rxSingleOutcome { eligibilityService.getProductEligibility(EligibleProduct.DEPOSIT_INTEREST) }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.WithdrawFiat ->
                rxSingleOutcome { eligibilityService.getProductEligibility(EligibleProduct.WITHDRAW_FIAT) }
                    .map(ProductEligibility::toFeatureAccess)
            is Feature.Interest,
            Feature.SimplifiedDueDiligence,
            is Feature.TierLevel -> TODO("Not Implemented Yet")
        }
    }

    override fun majorProductsNotEligibleReasons(): Single<List<ProductNotEligibleReason>> =
        rxSingleOutcome { eligibilityService.getMajorProductsNotEligibleReasons() }

    private fun NabuUser.toBasicProfileInfo() =
        BasicProfileInfo(
            firstName = firstName ?: email,
            lastName = lastName ?: email,
            email = email
        )

    override fun isKycResubmissionRequired(): Single<Boolean> =
        userService.getUser().map { it.isMarkedForResubmission }

    override fun shouldResubmitAfterRecovery(): Single<Boolean> =
        userService.getUser().map { it.isMarkedForRecoveryResubmission }

    override fun checkForUserWalletLinkErrors(): Completable =
        userService.getUser().flatMapCompletable { Completable.complete() }

    override fun isArgentinian(): Single<Boolean> =
        userService.getUser().zipWith(bindFeatureFlag.enabled).flatMap { (user, isBindEnabled) ->
            Single.just(user.address?.countryCode == COUNTRY_CODE_ARGENTINA && isBindEnabled)
        }

    override fun isCowboysUser(): Single<Boolean> =
        userService.getUser().map { user ->
            user.isCowboysUser
        }

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

    private companion object {
        private const val COUNTRY_CODE_ARGENTINA = "AR"
    }
}

private fun ProductEligibility.toFeatureAccess(): FeatureAccess =
    if (canTransact) FeatureAccess.Granted(maxTransactionsCap)
    else FeatureAccess.Blocked(
        when (val reason = reasonNotEligible) {
            ProductNotEligibleReason.InsufficientTier.Tier1TradeLimitExceeded ->
                BlockedReason.InsufficientTier.Tier1TradeLimitExceeded
            ProductNotEligibleReason.InsufficientTier.Tier2Required ->
                BlockedReason.InsufficientTier.Tier2Required
            is ProductNotEligibleReason.InsufficientTier.Unknown ->
                BlockedReason.InsufficientTier.Unknown(reason.message)
            ProductNotEligibleReason.Sanctions.RussiaEU5 ->
                BlockedReason.Sanctions.RussiaEU5
            is ProductNotEligibleReason.Sanctions.Unknown ->
                BlockedReason.Sanctions.Unknown(reason.message)
            is ProductNotEligibleReason.Unknown -> BlockedReason.NotEligible
            null -> BlockedReason.NotEligible
        }
    )
