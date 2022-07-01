package com.blockchain.core.chains.erc20.domain

import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface Erc20L2StoreService {
    fun getBalances(networkTicker: String): Observable<Map<AssetInfo, Erc20Balance>>

    fun getBalanceFor(networkTicker: String, asset: AssetInfo): Observable<Erc20Balance>

    fun getActiveAssets(networkTicker: String): Single<Set<AssetInfo>>
}
