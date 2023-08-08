package com.blockchain.home.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money

interface FiltersService {
    fun filters(): List<AssetFilter>
    fun updateFilters(filter: List<AssetFilter>)
}

sealed interface AssetFilter {
    fun shouldFilterOut(asset: AssetBalance): Boolean

    data class ShowSmallBalances(val enabled: Boolean) : AssetFilter {
        override fun shouldFilterOut(asset: AssetBalance): Boolean =
            enabled || !asset.isSmallBalance()
    }

    data class SearchFilter(private val query: String = "") : AssetFilter {
        override fun shouldFilterOut(asset: AssetBalance): Boolean {
            return query.isEmpty() ||
                asset.singleAccount.currency.name.contains(query, true) ||
                asset.singleAccount.currency.networkTicker.contains(query, true) ||
                asset.singleAccount.currency.displayTicker.contains(query, true)
        }
    }
}

/**
 * balance for each account,
 * e.g. for custodial BTC might have trading account, staking account, etc..
 */
data class SingleAccountBalance(
    val singleAccount: SingleAccount,
    val balance: DataResource<Money>,
    val fiatBalance: DataResource<Money?>,
    val majorCurrencyRate: DataResource<ExchangeRate?>,
    val exchangeRate24hWithDelta: DataResource<Prices24HrWithDelta?>
) {
    val majorCurrencyBalance: DataResource<Money?>
        get() = combineDataResources(balance, majorCurrencyRate) { balance, majorRate ->
            majorRate?.convert(balance)
        }
}

/**
 * balance for full asset,
 * e.g. for custodial BTC this combines trading account, staking account, etc..
 */
data class AssetBalance(
    val singleAccount: SingleAccount,
    val balance: DataResource<Money>,
    val fiatBalance: DataResource<Money?>,
    val majorCurrencyBalance: DataResource<Money?>,
    val exchangeRate24hWithDelta: DataResource<Prices24HrWithDelta?>
)

fun AssetBalance.isSmallBalance(): Boolean {
    return (majorCurrencyBalance as? DataResource.Data<Money?>)?.data?.let { majorCurrencyBalance ->
        majorCurrencyBalance < Money.fromMajor(majorCurrencyBalance.currency, 1.toBigDecimal())
    } ?: true
}
