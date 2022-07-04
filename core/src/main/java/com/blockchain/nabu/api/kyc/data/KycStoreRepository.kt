package com.blockchain.nabu.api.kyc.data

import com.blockchain.nabu.api.kyc.data.store.KycDataSource
import com.blockchain.nabu.api.kyc.domain.KycStoreService
import com.blockchain.nabu.common.extensions.wrapErrorMessage
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.Limits
import com.blockchain.nabu.models.responses.nabu.LimitsJson
import com.blockchain.nabu.models.responses.nabu.Tier
import com.blockchain.nabu.models.responses.nabu.TierResponse
import com.blockchain.nabu.models.responses.nabu.Tiers
import com.blockchain.store.asObservable
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

internal class KycStoreRepository(
    private val kycDataSource: KycDataSource,
    private val assetCatalogue: AssetCatalogue,
) : KycStoreService {

    private fun getKycTiers(refresh: Boolean): Observable<KycTiers> {
        return kycDataSource.stream(refresh = refresh)
            .mapData { tiersResponse ->
                KycTiers(
                    constructTierMap(tiersResponse.tiers)
                )
            }
            .asObservable { it }
            .wrapErrorMessage()
    }

    override fun getKycTiers(): Single<KycTiers> =
        getKycTiers(refresh = false).firstElement().toSingle()

    private fun constructTierMap(tiersResponse: List<TierResponse>): Tiers =
        KycTierLevel.values().map { level ->
            val tierResponse = tiersResponse[level.ordinal]
            val limitsCurrency = tierResponse.limits?.currency?.let {
                assetCatalogue.fromNetworkTicker(it)
            }
            level to Tier(
                tierResponse.state,
                Limits(
                    dailyLimit = tierResponse.limits?.dailyLimit(limitsCurrency),
                    annualLimit = tierResponse.limits?.annualLimit(limitsCurrency)
                )
            )
        }.toTiersMap()

    private fun LimitsJson.dailyLimit(limitsCurrency: Currency?): Money? {
        val currency = limitsCurrency ?: return null
        val amount = daily ?: return null
        return Money.fromMajor(currency, amount)
    }

    private fun LimitsJson.annualLimit(limitsCurrency: Currency?): Money? {
        val currency = limitsCurrency ?: return null
        val amount = annual ?: return null
        return Money.fromMajor(currency, amount)
    }

    private fun List<Pair<KycTierLevel, Tier>>.toTiersMap(): Tiers = Tiers(this.toMap())
}
