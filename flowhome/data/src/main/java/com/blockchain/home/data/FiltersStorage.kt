package com.blockchain.home.data

import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.domain.FiltersService
import com.blockchain.preferences.SmallBalancesPrefs

class FiltersStorage(private val smallBalancesPrefs: SmallBalancesPrefs) : FiltersService {
    override fun filters(): List<AssetFilter> {
        return listOf(
            AssetFilter.ShowSmallBalances(
                smallBalancesPrefs.showSmallBalances
            )
        )
    }

    override fun updateFilters(filter: List<AssetFilter>) {
        filter.forEach {
            if (it is AssetFilter.ShowSmallBalances)
                smallBalancesPrefs.showSmallBalances = it.enabled
        }
    }
}
