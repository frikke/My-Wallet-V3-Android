package com.blockchain.home.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.ui.HomeActivity
import info.blockchain.balance.Money

data class HomeViewState(
    val balance: DataResource<Money>,
    val cryptoAssets: DataResource<List<HomeCryptoAsset>>,
    val fiatAssets: DataResource<List<HomeFiatAsset>>,
    val activity: DataResource<List<HomeActivity>>
) : ViewState

sealed interface HomeAsset {
    val icon: String
    val name: String
    val balance: DataResource<Money>
}

data class HomeCryptoAsset(
    override val icon: String,
    override val name: String,
    override val balance: DataResource<Money>,
    val fiatBalance: DataResource<Money>,
    val change: DataResource<ValueChange>
) : HomeAsset

data class HomeFiatAsset(
    override val icon: String,
    override val name: String,
    override val balance: DataResource<Money>
) : HomeAsset
