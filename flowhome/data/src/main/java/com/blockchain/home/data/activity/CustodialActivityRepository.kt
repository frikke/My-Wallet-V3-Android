package com.blockchain.home.data.activity

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CustodialTransaction
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.home.activity.CustodialActivityService
import com.blockchain.store.mapData
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.rx3.asFlow

class CustodialActivityRepository(
    private val coincore: Coincore,
) : CustodialActivityService {
    private var activityCache: List<CustodialTransaction> = emptyList()
    override fun getAllActivity(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<CustodialTransaction>>> {
        return coincore.activeWalletsInMode(WalletMode.CUSTODIAL, freshnessStrategy).distinctUntilChanged { old, new ->
            val oldAssets = old.accounts.map { it.currency.networkTicker }
            val newAssets = new.accounts.map { it.currency.networkTicker }
            oldAssets.size == newAssets.size && oldAssets.toSet() == newAssets.toSet()
        }.flatMapLatest {
            it.activity(freshnessStrategy).asFlow().debounce(500).map { transactions ->
                require(
                    transactions.all { activitySummaryItem ->
                        activitySummaryItem is CustodialTransaction
                    }
                )
                DataResource.Data(transactions) as DataResource<List<CustodialTransaction>>
            }.distinctUntilChanged { old, new ->
                val oldData = (old as? DataResource.Data)?.data ?: return@distinctUntilChanged false
                val newData = (new as? DataResource.Data)?.data ?: return@distinctUntilChanged false
                val oldSet = oldData.map { item -> item.txId to item.state }.toSet()
                val newSet = newData.map { item -> item.txId to item.state }.toSet()
                return@distinctUntilChanged oldSet == newSet
            }.onEach {
                (it as? DataResource.Data)?.data?.let { txs ->
                    activityCache = txs
                }
            }
        }.catch {
            emit(DataResource.Error(it as Exception))
        }.onStart {
            activityCache.takeIf { cache -> cache.isNotEmpty() }?.let {
                emit(DataResource.Data(it))
            } ?: emit(DataResource.Loading)
        }
    }

    override fun getActivity(
        id: String,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<ActivitySummaryItem>> {
        return getAllActivity(freshnessStrategy)
            .take(1)
            .mapData { activityList ->
                id.split("|").let { txIdAndClass ->
                    activityList.first { it.txId == txIdAndClass[0] && it::class.toString() == txIdAndClass[1] }
                }
            }
            .catch {
                emit(DataResource.Error(Exception(it)))
            }
    }
}
