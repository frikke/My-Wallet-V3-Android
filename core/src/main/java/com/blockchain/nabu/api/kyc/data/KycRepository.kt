package com.blockchain.nabu.api.kyc.data

import com.blockchain.api.kyc.model.KycLimitsDto
import com.blockchain.api.kyc.model.KycTierDto
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.api.kyc.data.store.KycTiersStore
import com.blockchain.nabu.api.kyc.domain.KycService
import com.blockchain.nabu.api.kyc.domain.model.KycLimits
import com.blockchain.nabu.api.kyc.domain.model.KycTierDetail
import com.blockchain.nabu.api.kyc.domain.model.KycTierLevel
import com.blockchain.nabu.api.kyc.domain.model.KycTierState
import com.blockchain.nabu.api.kyc.domain.model.KycTiers
import com.blockchain.nabu.api.kyc.domain.model.TiersMap
import com.blockchain.nabu.common.extensions.wrapErrorMessage
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.asObservable

class KycRepository(
    private val kycTiersStore: KycTiersStore,
    private val assetCatalogue: AssetCatalogue,
) : KycService {

    private fun getKycTiersLegacyFlow(freshnessStrategy: FreshnessStrategy): Flow<KycTiers> {
        return kycTiersStore.stream(freshnessStrategy)
            .mapData { tiersResponse ->
                KycTiers(
                    constructTierMap(tiersResponse.tiers)
                )
            }
            .wrapErrorMessage()
            .getDataOrThrow()
    }

    // rx
    override fun getKycTiersLegacy(freshnessStrategy: FreshnessStrategy): Single<KycTiers> {
        return getKycTiersLegacyFlow(freshnessStrategy)
            .asObservable()
            .firstElement()
            .toSingle()
            .subscribeOn(Schedulers.io())
    }

    // flow
    override fun getKycTiers(refreshStrategy: FreshnessStrategy): Flow<KycTiers> {
        return getKycTiersLegacyFlow(refreshStrategy)
    }

    private fun constructTierMap(tiersResponse: List<KycTierDto>): TiersMap =
        KycTierLevel.values().map { level ->
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

private fun List<Pair<KycTierLevel, KycTierDetail>>.toTiersMap(): TiersMap = TiersMap(this.toMap())
