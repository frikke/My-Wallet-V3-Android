package com.blockchain.core.chains.erc20.data

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.core.chains.erc20.data.store.Erc20DataSource
import com.blockchain.core.chains.erc20.domain.Erc20StoreService
import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.store.asObservable
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

internal class Erc20StoreRepository(
    private val assetCatalogue: AssetCatalogue,
    private val erc20DataSource: Erc20DataSource
) : Erc20StoreService {

    private fun getBalances(refresh: Boolean): Observable<Map<AssetInfo, Erc20Balance>> {
        return erc20DataSource.stream(refresh = refresh)
            .mapData { balanceList ->
                balanceList.mapNotNull { balance ->
                    assetCatalogue.assetFromL1ChainByContractAddress(
                        CryptoCurrency.ETHER.networkTicker,
                        balance.contractAddress
                    )?.let { info ->
                        info to balance.mapBalance(info)
                    }
                }.toMap()
            }
            .asObservable { it }
            .onErrorReturn { emptyMap() }
    }

    override fun getBalances(): Observable<Map<AssetInfo, Erc20Balance>> =
        getBalances(refresh = true)

    override fun getBalanceFor(asset: AssetInfo): Observable<Erc20Balance> =
        getBalances(refresh = true)
            .map { it.getOrDefault(asset, Erc20Balance.zero(asset)) }

    override fun getActiveAssets(): Single<Set<AssetInfo>> =
        getBalances(refresh = false).map { it.keys }.firstElement().toSingle()
}

private fun Erc20TokenBalance?.mapBalance(asset: AssetInfo): Erc20Balance =
    this?.let {
        Erc20Balance(
            balance = CryptoValue.fromMinor(asset, it.balance),
            hasTransactions = it.transferCount > 0
        )
    } ?: Erc20Balance(
        balance = CryptoValue.zero(asset),
        hasTransactions = false
    )
