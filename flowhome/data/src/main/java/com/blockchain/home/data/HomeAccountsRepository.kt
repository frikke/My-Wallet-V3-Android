package com.blockchain.home.data

import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.home.model.AssetFilter
import com.blockchain.home.model.AssetFilterStatus
import com.blockchain.preferences.MultiAppAssetsPrefs
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class HomeAccountsRepository(
    private val coincore: Coincore,
    private val walletModeService: WalletModeService,
    private val multiAppAssetsPrefs: MultiAppAssetsPrefs
) : HomeAccountsService {
    override fun accounts(): Flow<DataResource<List<SingleAccount>>> {
        return walletModeService.walletMode.flatMapLatest { wMode ->
            coincore.activeWalletsInMode(wMode).map { it.accounts }
                .distinctUntilChanged { old, new ->
                    old.map { it.currency.networkTicker } == new.map { it.currency.networkTicker }
                }
                .map {
                    DataResource.Data(it) as DataResource<List<SingleAccount>>
                }.onStart {
                    emit(DataResource.Loading)
                }.catch {
                    DataResource.Error(it as Exception)
                }
        }
    }

    private var filtersFlow: MutableStateFlow<List<AssetFilterStatus>> = MutableStateFlow(
        multiAppAssetsPrefs.toFilterStatus()
    )

    override fun filters(): Flow<List<AssetFilterStatus>> {
        return filtersFlow
    }

    override fun updateFilters(filters: List<AssetFilterStatus>) {
        multiAppAssetsPrefs.fromFilterStatus(filters)
        filtersFlow.value = filters
    }

    private fun MultiAppAssetsPrefs.toFilterStatus(): List<AssetFilterStatus> {
        return AssetFilter.values().map { filter ->
            AssetFilterStatus(
                filter = filter,
                isEnabled = when (filter) {
                    AssetFilter.ShowSmallBalances -> shouldShowSmallBalances
                }
            )
        }
    }

    private fun MultiAppAssetsPrefs.fromFilterStatus(filters: List<AssetFilterStatus>) {
        filters.forEach { assetFilter ->
            when (assetFilter.filter) {
                AssetFilter.ShowSmallBalances -> shouldShowSmallBalances = assetFilter.isEnabled
            }
        }
    }
}
