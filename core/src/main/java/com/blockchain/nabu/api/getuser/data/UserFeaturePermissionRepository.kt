package com.blockchain.nabu.api.getuser.data

import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.combineDataResources
import com.blockchain.data.mapData
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class UserFeaturePermissionRepository(
    private val kycService: KycService,
    private val interestService: InterestService,
    private val eligibilityService: EligibilityService,
    private val simpleBuyService: SimpleBuyService
) : UserFeaturePermissionService {

    override fun isEligibleFor(
        feature: Feature,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> {
        return when (feature) {
            is Feature.TierLevel -> {
                kycService.getTiers(freshnessStrategy)
                    .mapData { it.isInitialisedFor(feature.tier).not() }
            }
            is Feature.Interest -> {
                interestService.getEligibilityForAssets(freshnessStrategy)
                    .mapData { mapAssetWithEligibility -> mapAssetWithEligibility.containsKey(feature.currency) }
            }
            Feature.Buy,
            Feature.Swap,
            Feature.Sell,
            Feature.DepositCrypto,
            Feature.DepositFiat,
            Feature.Dex,
            Feature.DepositInterest,
            Feature.DepositStaking,
            Feature.DepositActiveRewards,
            Feature.CustodialAccounts,
            Feature.Kyc,
            Feature.WithdrawFiat -> {
                getAccessForFeature(feature, freshnessStrategy).mapData { it is FeatureAccess.Granted }
            }
        }
    }

    override fun getAccessForFeature(
        feature: Feature,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<FeatureAccess>> {
        return when (feature) {
            Feature.Buy -> {
                combine(
                    eligibilityService.getProductEligibility(EligibleProduct.BUY, freshnessStrategy),
                    simpleBuyService.getEligibility(freshnessStrategy)
                ) { buyEligibility, simpleBuyEligibility ->
                    combineDataResources(
                        buyEligibility,
                        simpleBuyEligibility
                    ) { buyEligibilityData, simpleBuyEligibilityData ->
                        when {
                            buyEligibilityData.toFeatureAccess() !is FeatureAccess.Granted -> {
                                buyEligibilityData.toFeatureAccess()
                            }

                            simpleBuyEligibilityData.isPendingDepositThresholdReached.not() -> {
                                FeatureAccess.Granted()
                            }

                            else -> {
                                FeatureAccess.Blocked(
                                    BlockedReason.TooManyInFlightTransactions(
                                        simpleBuyEligibilityData.maxPendingDepositSimpleBuyTrades
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Feature.Swap -> {
                eligibilityService.getProductEligibility(
                    EligibleProduct.SWAP,
                    freshnessStrategy
                )
                    .mapData(ProductEligibility::toFeatureAccess)
            }

            Feature.Sell -> {
                eligibilityService.getProductEligibility(
                    EligibleProduct.SELL,
                    freshnessStrategy
                )
                    .mapData(ProductEligibility::toFeatureAccess)
            }

            Feature.DepositFiat -> {
                eligibilityService.getProductEligibility(
                    EligibleProduct.DEPOSIT_FIAT,
                    freshnessStrategy
                )
                    .mapData(ProductEligibility::toFeatureAccess)
            }

            Feature.DepositCrypto -> {
                eligibilityService.getProductEligibility(
                    EligibleProduct.DEPOSIT_CRYPTO,
                    freshnessStrategy
                )
                    .mapData(ProductEligibility::toFeatureAccess)
            }

            Feature.DepositInterest -> {
                eligibilityService.getProductEligibility(
                    EligibleProduct.DEPOSIT_INTEREST,
                    freshnessStrategy
                )
                    .mapData(ProductEligibility::toFeatureAccess)
            }

            Feature.DepositStaking -> {
                eligibilityService.getProductEligibility(
                    EligibleProduct.DEPOSIT_STAKING,
                    freshnessStrategy
                )
                    .mapData(ProductEligibility::toFeatureAccess)
            }

            Feature.DepositActiveRewards -> {
                eligibilityService.getProductEligibility(
                    EligibleProduct.DEPOSIT_EARN_CC1W,
                    freshnessStrategy
                )
                    .mapData(ProductEligibility::toFeatureAccess)
            }

            Feature.CustodialAccounts -> {
                eligibilityService.getProductEligibility(EligibleProduct.USE_CUSTODIAL_ACCOUNTS, freshnessStrategy)
                    .mapData(ProductEligibility::toFeatureAccess)
            }

            Feature.WithdrawFiat -> {
                eligibilityService.getProductEligibility(EligibleProduct.WITHDRAW_FIAT, freshnessStrategy)
                    .mapData(ProductEligibility::toFeatureAccess)
            }

            Feature.Kyc -> {
                eligibilityService.getProductEligibility(EligibleProduct.KYC, freshnessStrategy)
                    .mapData(ProductEligibility::toFeatureAccess)
            }
            Feature.Dex -> {
                eligibilityService.getProductEligibility(EligibleProduct.DEX, freshnessStrategy)
                    .mapData(ProductEligibility::toFeatureAccess)
            }
            is Feature.Interest,
            is Feature.TierLevel -> {
                TODO("Not Implemented Yet")
            }
        }
    }

    override fun getAccessForFeatures(
        vararg features: Feature,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<Feature, FeatureAccess>>> {
        return features
            .map { feature ->
                getAccessForFeature(feature = feature, freshnessStrategy = freshnessStrategy)
                    .mapData { featureAccess -> Pair(feature, featureAccess) }
            }.run {
                combine(this) { featureWithAccessPairArray ->
                    combineDataResources(featureWithAccessPairArray.toList()) {
                        mapOf(*it.toTypedArray())
                    }
                }
            }
    }
}

private fun ProductEligibility.toFeatureAccess(): FeatureAccess {
    return if (canTransact) {
        FeatureAccess.Granted(maxTransactionsCap)
    } else {
        FeatureAccess.Blocked(
            when (val reason = reasonNotEligible) {
                ProductNotEligibleReason.InsufficientTier.Tier1TradeLimitExceeded -> {
                    BlockedReason.InsufficientTier.Tier1TradeLimitExceeded
                }
                ProductNotEligibleReason.InsufficientTier.Tier1Required -> {
                    BlockedReason.InsufficientTier.Tier1Required
                }
                ProductNotEligibleReason.InsufficientTier.Tier2Required -> {
                    BlockedReason.InsufficientTier.Tier2Required
                }
                is ProductNotEligibleReason.InsufficientTier.Unknown -> {
                    BlockedReason.InsufficientTier.Unknown(reason.message)
                }
                is ProductNotEligibleReason.Sanctions.RussiaEU5 -> {
                    BlockedReason.Sanctions.RussiaEU5(reason.message)
                }
                is ProductNotEligibleReason.Sanctions.RussiaEU8 -> {
                    BlockedReason.Sanctions.RussiaEU8(reason.message)
                }
                is ProductNotEligibleReason.Sanctions.Unknown -> {
                    BlockedReason.Sanctions.Unknown(reason.message)
                }
                is ProductNotEligibleReason.Unknown -> {
                    BlockedReason.NotEligible(reason.message)
                }
                null -> {
                    BlockedReason.NotEligible(null)
                }
            }
        )
    }
}
