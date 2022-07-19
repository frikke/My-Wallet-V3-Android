package com.blockchain.core.interest

import com.blockchain.core.interest.data.store.InterestDataSource
import com.blockchain.core.interest.domain.InterestStoreService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
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

    fun getActiveAssets(forceRefresh: Boolean): Single<Set<AssetInfo>>
    fun flushCaches(asset: AssetInfo)
}

internal class InterestBalanceDataManagerImpl(
    private val interestStoreService: InterestStoreService,
    private val interestDataSource: InterestDataSource
) : InterestBalanceDataManager {
    override fun getBalanceForAsset(asset: AssetInfo): Observable<InterestAccountBalance> =
        interestStoreService.getBalanceFor(asset = asset)

    override fun getActiveAssets(forceRefresh: Boolean): Single<Set<AssetInfo>> =
        interestStoreService.getActiveAssets(forceRefresh)

    override fun flushCaches(asset: AssetInfo) =
        interestDataSource.invalidate()
}
