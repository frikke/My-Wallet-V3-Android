package com.blockchain.home.presentation.allassets

import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.home.domain.AssetFilter
import com.blockchain.presentation.balance.WalletBalance
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

data class AssetsViewState(
    val balance: WalletBalance,
    val assets: DataResource<List<HomeAsset>>,
    val filters: List<AssetFilter>,
    val showNoResults: Boolean,
    val showFilterIcon: Boolean,
    val fundsLocks: DataResource<FundsLocks?>
) : ViewState

sealed interface HomeAsset {
    val icon: List<String>
    val name: String
    val balance: DataResource<Money>
    val fiatBalance: DataResource<Money?>
}

data class CustodialAssetState(
    override val asset: AssetInfo,
    override val icon: List<String>,
    override val name: String,
    override val balance: DataResource<Money>,
    override val fiatBalance: DataResource<Money?>,
    val change: DataResource<ValueChange>
) : HomeCryptoAsset

data class NonCustodialAssetState(
    override val asset: AssetInfo,
    override val icon: List<String>,
    override val name: String,
    override val balance: DataResource<Money>,
    override val fiatBalance: DataResource<Money?>
) : HomeCryptoAsset

data class FiatAssetState(
    override val icon: List<String>,
    override val name: String,
    override val balance: DataResource<Money>,
    override val fiatBalance: DataResource<Money?>,
    val account: FiatAccount
) : HomeAsset

interface HomeCryptoAsset : HomeAsset {
    val asset: AssetInfo
}
