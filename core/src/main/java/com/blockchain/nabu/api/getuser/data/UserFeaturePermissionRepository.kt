package com.blockchain.nabu.api.getuser.data

import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.sdd.domain.SddService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.toFeatureAccess
import com.blockchain.store.mapData
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

internal class UserFeaturePermissionRepository(
    private val getUserStore: GetUserStore,
    private val kycService: KycService,
    private val interestService: InterestService,
    private val sddService: SddService
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
            is Feature.SimplifiedDueDiligence -> {
                sddService.isEligible(freshnessStrategy)
            }
            Feature.Buy,
            Feature.Swap,
            Feature.Sell,
            Feature.DepositCrypto,
            Feature.DepositFiat,
            Feature.DepositInterest,
            Feature.WithdrawFiat -> {
                getAccessForFeature(feature).mapData { it is FeatureAccess.Granted }
            }
        }
    }

    override fun getAccessForFeature(
        feature: Feature,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<FeatureAccess>> {

          when (feature) {
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
}
