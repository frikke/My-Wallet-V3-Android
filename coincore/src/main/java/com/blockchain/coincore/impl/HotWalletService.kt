package com.blockchain.coincore.impl

import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.ExchangeAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.core.walletoptions.WalletOptionsStore
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.asSingle
import com.blockchain.data.onErrorReturn
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.isLayer2Token
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.rxjava3.core.Single

class HotWalletService(
    private val walletOptionsStore: WalletOptionsStore
) {
    // When the feature is not enabled, or no hot wallet address is found for the given product/currency
    // return the original target address. This will mean no change in behaviour.
    fun resolveReceiveAddress(
        sourceCurrency: Currency,
        target: TransactionTarget,
        isSwap: Boolean = false
    ): Single<String> {
        return walletOptionsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .onErrorReturn { WalletOptions() }.asSingle()
            .map { walletOptions ->
                val product = resolveProduct(target, isSwap)
                walletOptions.hotWalletAddresses[product.name.lowercase()]?.let { addressesForProduct ->
                    addressesForProduct[getAssetNetworkTicker(sourceCurrency).lowercase()] ?: EMPTY_ADDRESS
                } ?: EMPTY_ADDRESS
            }.onErrorReturn { "" }
    }

    private fun resolveProduct(
        target: TransactionTarget,
        isSwap: Boolean
    ): Product {
        return when (target) {
            is EarnRewardsAccount.Interest -> Product.REWARDS
            is TradingAccount -> if (isSwap) {
                Product.SWAP
            } else {
                Product.SIMPLEBUY
            }

            is ExchangeAccount -> Product.EXCHANGE
            else -> Product.NONE
        }
    }

    private fun getAssetNetworkTicker(currency: Currency) =
        if (currency.isLayer2Token) {
            (currency as AssetInfo).coinNetwork!!.networkTicker
        } else {
            currency.networkTicker
        }

    private enum class Product {
        NONE,
        EXCHANGE,
        LENDING,
        REWARDS,
        SIMPLEBUY,
        SWAP
    }

    companion object {
        private const val EMPTY_ADDRESS = ""
    }
}
