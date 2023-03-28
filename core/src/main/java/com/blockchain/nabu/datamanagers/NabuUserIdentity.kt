package com.blockchain.nabu.datamanagers

import com.blockchain.api.NabuApiException
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.extensions.exhaustive
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.LinkedError
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.asSingle
import com.blockchain.utils.rxSingleOutcome
import com.blockchain.utils.zipSingles
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.coroutines.rx3.asObservable

class NabuUserIdentity(
    private val interestService: InterestService,
    private val simpleBuyService: SimpleBuyService,
    private val kycService: KycService,
    private val userService: UserService,
    private val eligibilityService: EligibilityService,
    private val bindFeatureFlag: FeatureFlag
) : UserIdentity {
    override fun isEligibleFor(feature: Feature, freshnessStrategy: FreshnessStrategy): Single<Boolean> {
        return when (feature) {
            is Feature.TierLevel -> kycService.getTiersLegacy().map {
                it.isInitialisedFor(feature.tier).not()
            }
            is Feature.Interest -> interestService.getEligibilityForAssetsLegacy()
                .map { mapAssetWithEligibility -> mapAssetWithEligibility.containsKey(feature.currency) }
            Feature.Buy,
            Feature.Swap,
            Feature.Sell,
            Feature.DepositCrypto,
            Feature.DepositFiat,
            Feature.DepositInterest,
            Feature.DepositStaking,
            Feature.DepositActiveRewards,
            Feature.CustodialAccounts,
            Feature.WithdrawFiat -> userAccessForFeature(feature, freshnessStrategy).map { it is FeatureAccess.Granted }
        }
    }

    override fun isVerifiedFor(feature: Feature): Single<Boolean> {
        return when (feature) {
            is Feature.TierLevel -> kycService.getTiersLegacy().map {
                it.isApprovedFor(feature.tier)
            }
            is Feature.Interest,
            Feature.Buy,
            Feature.CustodialAccounts,
            Feature.DepositCrypto,
            Feature.Swap,
            Feature.DepositFiat,
            Feature.DepositInterest,
            Feature.DepositStaking,
            Feature.DepositActiveRewards,
            Feature.Sell,
            Feature.WithdrawFiat -> throw IllegalArgumentException("Cannot be verified for $feature")
        }.exhaustive
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
            val state = it.address?.stateIso
            if (state.isNullOrEmpty()) {
                Maybe.empty()
            } else {
                Maybe.just(state)
            }
        }

    override fun userAccessForFeatures(
        features: List<Feature>,
        freshnessStrategy: FreshnessStrategy
    ): Single<Map<Feature, FeatureAccess>> =
        features.map { feature ->
            userAccessForFeature(feature, freshnessStrategy).map { access ->
                Pair(feature, access)
            }
        }.zipSingles()
            .map { mapOf(*it.toTypedArray()) }

    override fun userAccessForFeature(feature: Feature, freshnessStrategy: FreshnessStrategy): Single<FeatureAccess> {
        return when (feature) {
            Feature.Buy ->
                Single.zip(
                    rxSingleOutcome {
                        eligibilityService.getProductEligibilityLegacy(
                            EligibleProduct.BUY, freshnessStrategy
                        )
                    },
                    simpleBuyService.getEligibility(freshnessStrategy).asSingle()
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
                rxSingleOutcome {
                    eligibilityService.getProductEligibilityLegacy(
                        EligibleProduct.SWAP, freshnessStrategy
                    )
                }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.Sell ->
                rxSingleOutcome {
                    eligibilityService.getProductEligibilityLegacy(
                        EligibleProduct.SELL, freshnessStrategy
                    )
                }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.DepositFiat ->
                rxSingleOutcome {
                    eligibilityService.getProductEligibilityLegacy(
                        EligibleProduct.DEPOSIT_FIAT, freshnessStrategy
                    )
                }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.DepositCrypto ->
                rxSingleOutcome {
                    eligibilityService.getProductEligibilityLegacy(
                        EligibleProduct.DEPOSIT_CRYPTO, freshnessStrategy
                    )
                }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.DepositInterest ->
                rxSingleOutcome {
                    eligibilityService.getProductEligibilityLegacy(
                        EligibleProduct.DEPOSIT_INTEREST, freshnessStrategy
                    )
                }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.WithdrawFiat ->
                rxSingleOutcome {
                    eligibilityService.getProductEligibilityLegacy(
                        EligibleProduct.WITHDRAW_FIAT, freshnessStrategy
                    )
                }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.DepositStaking ->
                rxSingleOutcome {
                    eligibilityService.getProductEligibilityLegacy(
                        EligibleProduct.DEPOSIT_STAKING, freshnessStrategy
                    )
                }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.DepositActiveRewards ->
                rxSingleOutcome {
                    eligibilityService.getProductEligibilityLegacy(
                        EligibleProduct.DEPOSIT_EARN_CC1W, freshnessStrategy
                    )
                }
                    .map(ProductEligibility::toFeatureAccess)
            Feature.CustodialAccounts ->
                rxSingleOutcome {
                    eligibilityService.getProductEligibilityLegacy(
                        EligibleProduct.USE_CUSTODIAL_ACCOUNTS, freshnessStrategy
                    )
                }
                    .map(ProductEligibility::toFeatureAccess)
            is Feature.Interest,
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

    override fun userLinkedError(): Maybe<LinkedError> {
        return userService.getUser().flatMapMaybe<LinkedError> {
            Maybe.empty()
        }.onErrorResumeNext {
            if ((it as? NabuApiException)?.isUserWalletLinkError() == true) {
                Maybe.just(
                    LinkedError(
                        it.getErrorDescription().split(NabuApiException.USER_WALLET_LINK_ERROR_PREFIX).last()
                    )
                )
            } else Maybe.error(it)
        }
    }

    override fun isSSO(): Single<Boolean> =
        userService.getUserFlow(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .asObservable()
            .firstOrError()
            .map { user ->
                user.isSSO
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
            ProductNotEligibleReason.InsufficientTier.Tier1Required ->
                BlockedReason.InsufficientTier.Tier1Required
            ProductNotEligibleReason.InsufficientTier.Tier2Required ->
                BlockedReason.InsufficientTier.Tier2Required
            is ProductNotEligibleReason.InsufficientTier.Unknown ->
                BlockedReason.InsufficientTier.Unknown(reason.message)
            is ProductNotEligibleReason.Sanctions.RussiaEU5 ->
                BlockedReason.Sanctions.RussiaEU5(reason.message)
            is ProductNotEligibleReason.Sanctions.RussiaEU8 ->
                BlockedReason.Sanctions.RussiaEU8(reason.message)
            is ProductNotEligibleReason.Sanctions.Unknown ->
                BlockedReason.Sanctions.Unknown(reason.message)
            is ProductNotEligibleReason.Unknown -> BlockedReason.NotEligible(reason.message)
            null -> BlockedReason.NotEligible(null)
        }
    )
