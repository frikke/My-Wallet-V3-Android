package com.blockchain.home.data

import android.content.SharedPreferences
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.domain.FiltersService

class FiltersStorage(private val sharedPreferences: SharedPreferences) : FiltersService {
    override fun filters(): List<AssetFilter> {
        return listOf(
            AssetFilter.ShowSmallBalances(
                sharedPreferences.getBoolean(SHOULD_SHOW_SMALL_BALANCES, false)
            )
        )
    }

    override fun updateFilters(filter: List<AssetFilter>) {
        filter.forEach {
            if (it is AssetFilter.ShowSmallBalances)
                sharedPreferences.edit().putBoolean(SHOULD_SHOW_SMALL_BALANCES, it.enabled).apply()
        }
    }
}

private const val SHOULD_SHOW_SMALL_BALANCES = "should_show_small_balances"
