package com.blockchain.core.chains.erc20.domain

import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface Erc20L2StoreService {
    fun getBalances(
        accountHash: String,
        networkTicker: String
    ): Observable<Map<AssetInfo, Erc20Balance>>

    fun getBalanceFor(
        accountHash: String,
        networkTicker: String,
        asset: AssetInfo
    ): Observable<Erc20Balance>

    fun getActiveAssets(
        accountHash: String,
        networkTicker: String
    ): Single<Set<AssetInfo>>
}
