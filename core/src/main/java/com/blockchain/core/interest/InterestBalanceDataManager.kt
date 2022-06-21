package com.blockchain.core.interest

import com.blockchain.core.interest.domain.InterestStoreService
import com.blockchain.featureflag.FeatureFlag
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

data class InterestAccountBalance(
    val totalBalance: Money,
    val pendingInterest: Money,
    val pendingDeposit: Money,
    val totalInterest: Money,
    val lockedBalance: Money,
    val hasTransactions: Boolean = false,
) {
    val actionableBalance: CryptoValue
        get() = (totalBalance - lockedBalance) as CryptoValue
}

interface InterestBalanceDataManager {
    fun getBalanceForAsset(asset: AssetInfo): Observable<InterestAccountBalance>

    fun getActiveAssets(): Single<Set<AssetInfo>>
    fun flushCaches(asset: AssetInfo)
}

internal class InterestBalanceDataManagerImpl(
    private val balanceCallCache: InterestBalanceCallCache,
    private val interestStoreService: InterestStoreService,
    private val speedUpLoginInterestFF: FeatureFlag,
) : InterestBalanceDataManager {
    override fun getBalanceForAsset(asset: AssetInfo): Observable<InterestAccountBalance> {
        return speedUpLoginInterestFF.enabled.flatMapObservable { isEnabled ->
            if (isEnabled) {
                interestStoreService.getBalanceFor(asset = asset)
            } else {
                balanceCallCache.getBalances().map {
                    it.getOrDefault(asset, zeroBalance(asset))
                }.toObservable()
            }
        }
    }

    override fun getActiveAssets(): Single<Set<AssetInfo>> {
        return speedUpLoginInterestFF.enabled.flatMap { isEnabled ->
            if (isEnabled) {
                interestStoreService.getActiveAssets()
            } else {
                balanceCallCache.getBalances().map { it.keys }
            }
        }
    }

    override fun flushCaches(asset: AssetInfo) {
        speedUpLoginInterestFF.enabled.subscribe { isEnabled ->
            if (isEnabled) {
                interestStoreService.invalidate()
            } else {
                balanceCallCache.invalidate()
            }
        }
    }
}

private fun zeroBalance(asset: Currency): InterestAccountBalance =
    InterestAccountBalance(
        totalBalance = Money.zero(asset),
        pendingInterest = Money.zero(asset),
        pendingDeposit = Money.zero(asset),
        totalInterest = Money.zero(asset),
        lockedBalance = Money.zero(asset)
    )
