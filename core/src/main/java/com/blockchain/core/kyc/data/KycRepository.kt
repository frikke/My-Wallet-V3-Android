package com.blockchain.core.kyc.data

import com.blockchain.api.kyc.KycApiService
import com.blockchain.api.kyc.model.KycLimitsDto
import com.blockchain.api.kyc.model.KycTierDto
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycLimits
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierDetail
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.core.kyc.domain.model.TiersMap
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.getDataOrThrow
import com.blockchain.data.mapData
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.common.extensions.wrapErrorMessage
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable

class KycRepository(
    private val kycTiersStore: KycTiersStore,
    private val userService: UserService,
    private val assetCatalogue: AssetCatalogue,
    private val kycApiService: KycApiService,
    private val proveFeatureFlag: FeatureFlag
) : KycService {

    private fun getKycTiersFlow(freshnessStrategy: FreshnessStrategy): Flow<DataResource<KycTiers>> {
        return kycTiersStore.stream(freshnessStrategy)
            .mapData { tiersResponse ->
                KycTiers(
                    constructTierMap(tiersResponse.tiers)
                )
            }
            .wrapErrorMessage()
    }

    // rx
    override fun getTiersLegacy(freshnessStrategy: FreshnessStrategy): Single<KycTiers> {
        return getKycTiersFlow(freshnessStrategy).getDataOrThrow()
            .asObservable()
            .firstElement()
            .toSingle()
            .subscribeOn(Schedulers.io())
    }

    override fun stateFor(
        tierLevel: KycTier,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<KycTierState>> {
        return getKycTiersFlow(freshnessStrategy).mapData {
            it.stateFor(tierLevel)
        }
    }

    override fun getHighestApprovedTierLevelLegacy(freshnessStrategy: FreshnessStrategy): Single<KycTier> {
        return getTiersLegacy(freshnessStrategy).map { kycTiers ->
            val approvedTier = KycTier.values().reversed().find {
                kycTiers.isApprovedFor(it)
            }
            approvedTier ?: throw IllegalStateException("No approved tiers")
        }
    }

    override suspend fun shouldLaunchProve(): Outcome<Exception, Boolean> =
        // NOTE(aromano): currently disabled
        if (false && proveFeatureFlag.coEnabled()) {
            kycApiService.getKycFlow().map { response ->
                response?.nextFlow == "/kyc/prove"
            }
        } else {
            Outcome.Success(false)
        }

    override fun isPendingFor(tierLevel: KycTier, freshnessStrategy: FreshnessStrategy): Single<Boolean> {
        return getTiersLegacy(freshnessStrategy).map { kycTiers ->
            kycTiers.isPendingOrUnderReviewFor(tierLevel)
        }
    }

    override fun isRejected(freshnessStrategy: FreshnessStrategy): Single<Boolean> {
        return getTiersLegacy(freshnessStrategy).map { kycTiers ->
            KycTier.values().any { level ->
                kycTiers.isRejectedFor(level)
            }
        }
    }

    override fun isInProgress(freshnessStrategy: FreshnessStrategy): Single<Boolean> {
        return Single.zip(
            userService.getUserFlow(freshnessStrategy)
                .asObservable().firstOrError()
                .map { it.tiers?.next ?: 0 },
            getTiersLegacy(freshnessStrategy)
        ) { nextTier, kycTiers ->
            kycTiers.isInitialisedFor(KycTier.values()[nextTier]).not()
        }
    }

    override fun isRejectedFor(tierLevel: KycTier, freshnessStrategy: FreshnessStrategy): Single<Boolean> {
        return getTiersLegacy(freshnessStrategy).map {
            it.isRejectedFor(tierLevel)
        }
    }

    override fun isResubmissionRequired(freshnessStrategy: FreshnessStrategy): Single<Boolean> {
        return userService.getUserFlow(freshnessStrategy)
            .asObservable().firstOrError()
            .map { it.isMarkedForResubmission }
    }

    override fun shouldResubmitAfterRecovery(freshnessStrategy: FreshnessStrategy): Single<Boolean> {
        return userService.getUserFlow(freshnessStrategy)
            .asObservable().firstOrError()
            .map { it.isMarkedForRecoveryResubmission }
    }

    // flow
    override fun getTiers(refreshStrategy: FreshnessStrategy): Flow<DataResource<KycTiers>> {
        return getKycTiersFlow(refreshStrategy)
    }

    override fun getHighestApprovedTierLevel(freshnessStrategy: FreshnessStrategy): Flow<DataResource<KycTier>> {
        return getTiers(freshnessStrategy).mapData { kycTiers ->
            val approvedTier = KycTier.values().reversed().find {
                kycTiers.isApprovedFor(it)
            }
            approvedTier ?: throw IllegalStateException("No approved tiers")
        }
    }

    private fun constructTierMap(tiersResponse: List<KycTierDto>): TiersMap =
        KycTier.values().map { level ->
            val tierResponse = tiersResponse[level.ordinal]
            val limitsCurrency = tierResponse.limits?.currency?.let {
                assetCatalogue.fromNetworkTicker(it)
            }
            level to KycTierDetail(
                KycTierState.fromValue(tierResponse.state),
                KycLimits(
                    dailyLimit = tierResponse.limits?.dailyLimit(limitsCurrency),
                    annualLimit = tierResponse.limits?.annualLimit(limitsCurrency)
                )
            )
        }.toTiersMap()
}

// ///////////////
// EXTENSIONS
// ///////////////
private fun KycLimitsDto.dailyLimit(limitsCurrency: Currency?): Money? {
    val currency = limitsCurrency ?: return null
    val amount = daily ?: return null
    return Money.fromMajor(currency, amount)
}

private fun KycLimitsDto.annualLimit(limitsCurrency: Currency?): Money? {
    val currency = limitsCurrency ?: return null
    val amount = annual ?: return null
    return Money.fromMajor(currency, amount)
}

private fun List<Pair<KycTier, KycTierDetail>>.toTiersMap(): TiersMap = TiersMap(this.toMap())
