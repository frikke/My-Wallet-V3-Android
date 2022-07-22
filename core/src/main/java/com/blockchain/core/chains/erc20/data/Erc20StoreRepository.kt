package com.blockchain.core.chains.erc20.data

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.core.chains.erc20.data.store.Erc20DataSource
import com.blockchain.core.chains.erc20.domain.Erc20StoreService
import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.store.StoreRequest
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable

internal class Erc20StoreRepository(
    private val assetCatalogue: AssetCatalogue,
    private val erc20DataSource: Erc20DataSource
) : Erc20StoreService {

    private fun getBalancesFlow(request: StoreRequest): Flow<Map<AssetInfo, Erc20Balance>> {
        return erc20DataSource.streamData(request)
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
            .getDataOrThrow()
    }

    override fun getBalances(request: StoreRequest): Observable<Map<AssetInfo, Erc20Balance>> {
        return getBalancesFlow(request)
            .asObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(asset: AssetInfo, request: StoreRequest): Observable<Erc20Balance> {
        return getBalancesFlow(request)
            .asObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, Erc20Balance.zero(asset)) }
    }

    override fun getActiveAssets(request: StoreRequest): Flow<Set<AssetInfo>> {
        return getBalancesFlow(request)
            .map { it.keys }
    }
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
