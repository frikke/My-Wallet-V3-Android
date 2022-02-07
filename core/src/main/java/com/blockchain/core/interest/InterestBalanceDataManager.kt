package com.blockchain.core.interest

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
    val hasTransactions: Boolean = false
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
    private val balanceCallCache: InterestBalanceCallCache
) : InterestBalanceDataManager {
    override fun getBalanceForAsset(asset: AssetInfo): Observable<InterestAccountBalance> =
        balanceCallCache.getBalances().map {
            it.getOrDefault(asset, zeroBalance(asset))
        }.toObservable()

    override fun getActiveAssets(): Single<Set<AssetInfo>> =
        balanceCallCache.getBalances().map { it.keys }

    override fun flushCaches(asset: AssetInfo) {
        balanceCallCache.invalidate()
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
