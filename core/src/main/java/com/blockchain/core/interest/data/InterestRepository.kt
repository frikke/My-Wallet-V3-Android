package com.blockchain.core.interest.data

import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.core.interest.data.datasources.InterestAvailableAssetsTimedCache
import com.blockchain.core.interest.data.datasources.InterestBalancesStore
import com.blockchain.core.interest.data.datasources.InterestEligibilityTimedCache
import com.blockchain.core.interest.data.datasources.InterestLimitsTimedCache
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.core.interest.domain.model.InterestEligibility
import com.blockchain.core.interest.domain.model.InterestLimits
import com.blockchain.data.FreshnessStrategy
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable

internal class InterestRepository(
    private val assetCatalogue: AssetCatalogue,
    private val interestBalancesStore: InterestBalancesStore,
    private val interestEligibilityTimedCache: InterestEligibilityTimedCache,
    private val interestAvailableAssetsTimedCache: InterestAvailableAssetsTimedCache,
    private val interestLimitsTimedCache: InterestLimitsTimedCache,
) : InterestService {

    // balances
    private fun getBalancesFlow(refreshStrategy: FreshnessStrategy): Flow<Map<AssetInfo, InterestAccountBalance>> {
        return interestBalancesStore.stream(refreshStrategy)
            .mapData { interestBalanceDetailList ->
                interestBalanceDetailList.mapNotNull { interestBalanceDetails ->
                    (assetCatalogue.fromNetworkTicker(interestBalanceDetails.assetTicker) as? AssetInfo)
                        ?.let { assetInfo -> assetInfo to interestBalanceDetails.toInterestBalance(assetInfo) }
                }.toMap()
            }
            .getDataOrThrow()
    }

    override fun getBalances(refreshStrategy: FreshnessStrategy): Observable<Map<AssetInfo, InterestAccountBalance>> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Observable<InterestAccountBalance> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(refreshStrategy: FreshnessStrategy): Flow<Set<AssetInfo>> {
        return getBalancesFlow(refreshStrategy)
            .map { it.keys }
    }

    // availability
    override fun getAvailableAssetsForInterest(): Single<List<AssetInfo>> {
        return interestAvailableAssetsTimedCache.cached()
    }

    override fun isAssetAvailableForInterest(asset: AssetInfo): Single<Boolean> {
        return getAvailableAssetsForInterest()
            .map { assets -> assets.contains(asset) }
            .onErrorResumeNext { Single.just(false) }
    }

    // eligibility
    override fun getEligibilityForAssets(): Single<Map<AssetInfo, InterestEligibility>> {
        return interestEligibilityTimedCache.cached()
    }

    override fun getEligibilityForAsset(asset: AssetInfo): Single<InterestEligibility> {
        return getEligibilityForAssets().map { mapAssetWithEligibility ->
            mapAssetWithEligibility[asset] ?: InterestEligibility.Ineligible.default()
        }
    }

    // limits
    override fun getLimitsForAssets(): Single<Map<AssetInfo, InterestLimits>> {
        return interestLimitsTimedCache.cached()
    }

    override fun getLimitsForAsset(asset: AssetInfo): Single<InterestLimits> {
        return getLimitsForAssets().map { mapAssetWithLimits ->
            mapAssetWithLimits[asset] ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
        }
    }
}

private fun InterestBalanceDetails.toInterestBalance(asset: AssetInfo) =
    InterestAccountBalance(
        totalBalance = CryptoValue.fromMinor(asset, totalBalance),
        pendingInterest = CryptoValue.fromMinor(asset, pendingInterest),
        pendingDeposit = CryptoValue.fromMinor(asset, pendingDeposit),
        totalInterest = CryptoValue.fromMinor(asset, totalInterest),
        lockedBalance = CryptoValue.fromMinor(asset, lockedBalance),
        hasTransactions = true
    )

private fun zeroBalance(asset: Currency): InterestAccountBalance =
    InterestAccountBalance(
        totalBalance = Money.zero(asset),
        pendingInterest = Money.zero(asset),
        pendingDeposit = Money.zero(asset),
        totalInterest = Money.zero(asset),
        lockedBalance = Money.zero(asset)
    )
