package com.blockchain.core.interest.domain

import com.blockchain.core.interest.domain.model.InterestAccountBalance
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface InterestService {
    fun getBalances(): Observable<Map<AssetInfo, InterestAccountBalance>>
    fun getBalanceFor(asset: AssetInfo): Observable<InterestAccountBalance>
    fun getActiveAssets(refresh: Boolean = false): Single<Set<AssetInfo>>
}
