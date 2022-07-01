package com.blockchain.core.chains.erc20.data

import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.core.chains.erc20.data.store.Erc20L2DataSource
import com.blockchain.core.chains.erc20.domain.Erc20L2StoreService
import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.store.asObservable
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger

internal class Erc20L2StoreRepository(
    private val assetCatalogue: AssetCatalogue,
    private val erc20L2DataSource: Erc20L2DataSource
) : Erc20L2StoreService {

    private fun getBalances(
        accountHash: String,
        networkTicker: String,
        refresh: Boolean
    ): Observable<Map<AssetInfo, Erc20Balance>> {
        return erc20L2DataSource.stream(accountHash = accountHash, networkTicker = networkTicker, refresh = refresh)
            .mapData {
                it.addresses.firstOrNull { it.address == accountHash }
                    ?.balances?.mapNotNull { balance ->
                        val asset = if (balance.contractAddress == NonCustodialEvmService.NATIVE_IDENTIFIER) {
                            assetCatalogue.assetInfoFromNetworkTicker(balance.name)
                        } else {
                            assetCatalogue.assetFromL1ChainByContractAddress(
                                networkTicker,
                                balance.contractAddress
                            )
                        }

                        asset?.let {
                            asset to Erc20Balance(
                                balance = CryptoValue.fromMinor(asset, balance.amount),
                                hasTransactions = balance.amount > BigInteger.ZERO
                            )
                        }
                    }?.toMap() ?: emptyMap()
            }
            .asObservable { it }
            .onErrorReturn { emptyMap() }
    }

    override fun getBalances(
        accountHash: String,
        networkTicker: String,
    ): Observable<Map<AssetInfo, Erc20Balance>> =
        getBalances(accountHash = accountHash, networkTicker = networkTicker, refresh = true)

    override fun getBalanceFor(
        accountHash: String,
        networkTicker: String,
        asset: AssetInfo
    ): Observable<Erc20Balance> {
        return getBalances(accountHash = accountHash, networkTicker = networkTicker, refresh = true)
            .map { it.getOrDefault(asset, Erc20Balance.zero(asset)) }
    }

    override fun getActiveAssets(
        accountHash: String,
        networkTicker: String
    ): Single<Set<AssetInfo>> =
        getBalances(accountHash = accountHash, networkTicker = networkTicker, refresh = false)
            .map { it.keys }.firstElement().toSingle()

}
