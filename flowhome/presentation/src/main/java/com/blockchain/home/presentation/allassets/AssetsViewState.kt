package com.blockchain.home.presentation.allassets

import androidx.compose.ui.graphics.Color
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.data.dataOrElse
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.home.domain.AssetFilter
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.balance.percentageDelta

data class AssetsViewState(
    val balance: WalletBalance,
    val assets: DataResource<List<HomeAsset>>,
    val filters: List<AssetFilter>,
    val showNoResults: Boolean,
    val fundsLocks: DataResource<FundsLocks?>,
) : ViewState

sealed interface HomeAsset {
    val icon: List<String>
    val name: String
    val balance: DataResource<Money>
    val fiatBalance: DataResource<Money>
}

data class CustodialAssetState(
    override val asset: AssetInfo,
    override val icon: List<String>,
    override val name: String,
    override val balance: DataResource<Money>,
    override val fiatBalance: DataResource<Money>,
    val change: DataResource<ValueChange>
) : HomeCryptoAsset

data class NonCustodialAssetState(
    override val asset: AssetInfo,
    override val icon: List<String>,
    override val name: String,
    override val balance: DataResource<Money>,
    override val fiatBalance: DataResource<Money>,
) : HomeCryptoAsset

data class FiatAssetState(
    override val icon: List<String>,
    override val name: String,
    override val balance: DataResource<Money>,
    override val fiatBalance: DataResource<Money>,
    val account: FiatAccount
) : HomeAsset

interface HomeCryptoAsset : HomeAsset {
    val asset: AssetInfo
}

data class WalletBalance(
    val balance: DataResource<Money>,
    private val cryptoBalanceDifference24h: DataResource<Money>,
    private val cryptoBalanceNow: DataResource<Money>,
) {

    val balanceDifference: BalanceDifferenceConfig
        get() = combineDataResources(cryptoBalanceNow, cryptoBalanceDifference24h) { now, yesterday ->
            val difference = now.minus(yesterday)
            if (now.isZero && difference.isZero)
                BalanceDifferenceConfig()
            else
                ValueChange.fromValue(now.percentageDelta(yesterday)).takeIf { !it.value.isNaN() }
                    ?.let { valueChange ->
                        BalanceDifferenceConfig(
                            "${valueChange.indicator} " +
                                difference.toStringWithSymbol() +
                                " (${valueChange.value}%)",
                            valueChange.color
                        )
                    } ?: BalanceDifferenceConfig()
        }.dataOrElse(BalanceDifferenceConfig())
}

data class BalanceDifferenceConfig(val text: String = "", val color: Color = Color.Transparent)
