package com.blockchain.core.interest.domain

import com.blockchain.core.interest.InterestAccountBalance
import com.blockchain.storeservice.FlushableStoreService
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface InterestStoreService : FlushableStoreService {
    fun getBalances(): Observable<Map<AssetInfo, InterestAccountBalance>>
    fun getBalanceFor(asset: AssetInfo): Observable<InterestAccountBalance>
    fun getActiveAssets(): Single<Set<AssetInfo>>
}
