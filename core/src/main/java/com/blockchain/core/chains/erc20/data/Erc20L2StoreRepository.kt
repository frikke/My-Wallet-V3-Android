package com.blockchain.core.chains.erc20.data

import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.core.chains.erc20.data.store.Erc20L2DataSource
import com.blockchain.core.chains.erc20.data.store.Erc20L2Store
import com.blockchain.core.chains.erc20.domain.Erc20L2StoreService
import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Observable
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

internal class Erc20L2StoreRepository(
    private val assetCatalogue: AssetCatalogue,
    private val ethDataManager: EthDataManager,
    private val erc20L2DataSource: Erc20L2DataSource
) : Erc20L2StoreService {

    private fun getBalancesFlow(
        networkTicker: String,
        refreshStrategy: KeyedFreshnessStrategy<Erc20L2Store.Key>
    ): Flow<Map<AssetInfo, Erc20Balance>> {
        return erc20L2DataSource
            .streamData(refreshStrategy)
            .mapData {
                it.addresses.firstOrNull { it.address == ethDataManager.accountAddress }
                    ?.balances?.mapNotNull { balance ->
                        val asset = if (balance.contractAddress == NonCustodialEvmService.NATIVE_IDENTIFIER) {
                            assetCatalogue.assetInfoFromNetworkTicker(networkTicker)
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
            .getDataOrThrow()
    }

    override fun getBalances(
        networkTicker: String,
        refreshStrategy: FreshnessStrategy
    ): Observable<Map<AssetInfo, Erc20Balance>> {
        return getBalancesFlow(networkTicker, refreshStrategy.withKey(Erc20L2Store.Key(networkTicker)))
            .asObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(
        networkTicker: String,
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Observable<Erc20Balance> {
        return getBalancesFlow(networkTicker, refreshStrategy.withKey(Erc20L2Store.Key(networkTicker)))
            .asObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, Erc20Balance.zero(asset)) }
    }

    override fun getActiveAssets(
        networkTicker: String,
        refreshStrategy: FreshnessStrategy
    ): Flow<Set<AssetInfo>> {
        return getBalancesFlow(networkTicker, refreshStrategy.withKey(Erc20L2Store.Key(networkTicker)))
            .map { it.keys }
    }
}
