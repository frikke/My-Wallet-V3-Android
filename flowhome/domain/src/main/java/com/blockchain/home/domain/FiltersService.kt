package com.blockchain.home.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money

interface FiltersService {
    fun filters(): List<AssetFilter>
    fun updateFilters(filter: List<AssetFilter>)
}

sealed interface AssetFilter {
    fun shouldFilterOut(modelAccount: ModelAccount): Boolean

    data class ShowSmallBalances(val enabled: Boolean) : AssetFilter {
        override fun shouldFilterOut(modelAccount: ModelAccount): Boolean =
            !enabled ||
                (modelAccount.usdBalance as? DataResource.Data<Money>)?.data?.let {
                    it >= Money.fromMajor(
                        FiatCurrency.Dollars, 1.toBigDecimal()
                    )
                } ?: false
    }

    data class SearchFilter(private val query: String = "") : AssetFilter {
        override fun shouldFilterOut(modelAccount: ModelAccount): Boolean {
            return query.isEmpty() ||
                modelAccount.singleAccount.currency.networkTicker.contains(query, true) ||
                modelAccount.singleAccount.currency.displayTicker.contains(query, true)
        }
    }
}

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
