package com.blockchain.home.presentation.dashboard

import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import info.blockchain.balance.Money

/**
 * @property cryptoAssets <assets/isFullList>
 */
data class HomeViewState(
    val balance: DataResource<Money>,
    val cryptoAssets: DataResource<Pair<List<HomeCryptoAsset> /*display list*/, Boolean /*is full list*/>>,
    val fiatAssets: DataResource<List<HomeFiatAsset>>,
    val filters: List<CryptoAssetFilterStatus>
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

enum class CryptoAssetFilter(@get:StringRes val title: Int) {
    ShowSmallBalances(title = R.string.assets_filter_small_balances);

    companion object {
        val MinimumBalance = 1.toBigDecimal()
    }
}

data class CryptoAssetFilterStatus(
    val filter: CryptoAssetFilter,
    val isEnabled: Boolean
)