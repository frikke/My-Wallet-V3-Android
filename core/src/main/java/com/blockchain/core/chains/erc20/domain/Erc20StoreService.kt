package com.blockchain.core.chains.erc20.domain

import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface Erc20StoreService {
    fun getBalances(): Observable<Map<AssetInfo, Erc20Balance>>

    fun getBalanceFor(asset: AssetInfo): Observable<Erc20Balance>

    fun getActiveAssets(): Single<Set<AssetInfo>>
}
