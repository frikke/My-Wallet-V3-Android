package com.blockchain.core.chains.erc20.domain

import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface Erc20StoreService {
    fun getBalances(accountHash: String): Observable<Map<AssetInfo, Erc20Balance>>

    fun getBalanceFor(accountHash: String, asset: AssetInfo): Observable<Erc20Balance>

    fun getActiveAssets(accountHash: String): Single<Set<AssetInfo>>
}
