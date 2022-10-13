package com.blockchain.home.presentation.allassets

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.home.model.AssetFilterStatus
import info.blockchain.balance.Money

/**
 * @property cryptoAssets <assets/isFullList>
 */
data class AssetsViewState(
    val balance: DataResource<Money>,
    val cryptoAssets: DataResource<Pair<List<CryptoAssetState> /*display list*/, Boolean /*is full list*/>>,
    val fiatAssets: DataResource<List<FiatAssetState>>,
    val filters: List<AssetFilterStatus>
) : ViewState

sealed interface HomeAsset {
    val icon: String
    val name: String
    val balance: DataResource<Money>
}

data class CryptoAssetState(
    override val icon: String,
    override val name: String,
    override val balance: DataResource<Money>,
    val fiatBalance: DataResource<Money>,
    val change: DataResource<ValueChange>
) : HomeAsset

data class FiatAssetState(
    override val icon: String,
    override val name: String,
    override val balance: DataResource<Money>
) : HomeAsset