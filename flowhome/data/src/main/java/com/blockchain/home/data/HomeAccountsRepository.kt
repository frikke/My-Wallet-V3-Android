package com.blockchain.home.data

import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.home.model.AssetFilters
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

    private var filtersFlow: MutableStateFlow<AssetFilters> = MutableStateFlow(
        AssetFilters(shouldShowSmallBalances = multiAppAssetsPrefs.shouldShowSmallBalances)
    )

    override fun filters(): Flow<AssetFilters> {
        return filtersFlow
    }

    override fun updateFilters(filters: Boolean) {
        multiAppAssetsPrefs.shouldShowSmallBalances = filters
        filtersFlow.value = AssetFilters(shouldShowSmallBalances = multiAppAssetsPrefs.shouldShowSmallBalances)
    }
}
