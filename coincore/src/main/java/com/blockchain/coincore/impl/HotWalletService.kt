package com.blockchain.coincore.impl

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ExchangeAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.TradingAccount
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.isLayer2Token
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Single

class HotWalletService(
    private val walletApi: WalletApi
) {
    // When the feature is not enabled, or no hot wallet address is found for the given product/currency
    // return the original target address. This will mean no change in behaviour.
    fun resolveReceiveAddress(
        sourceCurrency: Currency,
        target: CryptoAccount,
        isSwap: Boolean = false
    ): Single<String> {
        return Single.fromObservable(walletApi.walletOptions).map { walletOptions ->
            val product = resolveProduct(target, isSwap)
            walletOptions.hotWalletAddresses[product.name.lowercase()]?.let { addressesForProduct ->
                addressesForProduct[getAssetNetworkTicker(sourceCurrency).lowercase()] ?: EMPTY_ADDRESS
            } ?: EMPTY_ADDRESS
        }
    }

    private fun resolveProduct(
        target: CryptoAccount,
        isSwap: Boolean
    ): Product {
        return when (target) {
            is InterestAccount -> Product.REWARDS
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
            CryptoCurrency.ETHER.networkTicker
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
