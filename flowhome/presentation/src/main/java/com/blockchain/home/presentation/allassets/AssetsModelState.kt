package com.blockchain.home.presentation.allassets

import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.home.model.AssetFilterStatus
import com.blockchain.home.presentation.SectionSize
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money

data class AssetsModelState(
    val accounts: DataResource<List<ModelAccount>> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val filterTerm: String = "",
    val filters: List<AssetFilterStatus> = listOf()
) : ModelState

data class ModelAccount(
    val singleAccount: SingleAccount,
    val balance: DataResource<Money>,
    val fiatBalance: DataResource<Money>,
    val usdRate: DataResource<ExchangeRate>,
    val exchangeRate24hWithDelta: DataResource<Prices24HrWithDelta>
) {
    val usdBalance: DataResource<Money>
        get() = combineDataResources(balance, usdRate) { balance, usdRate ->
            usdRate.convert(balance)
        }
}
