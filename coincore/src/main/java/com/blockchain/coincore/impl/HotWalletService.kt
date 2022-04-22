package com.blockchain.coincore.impl

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ExchangeAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.core.featureflag.IntegratedFeatureFlag
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.isErc20
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles

class HotWalletService(
    private val walletApi: WalletApi,
    private val ethMemoForHotWalletFeatureFlag: IntegratedFeatureFlag
) {
    // When the feature is not enabled, or no hot wallet address is found for the given product/currency
    // return the original target address. This will mean no change in behaviour.
    fun resolveReceiveAddress(
        sourceCurrency: Currency,
        target: CryptoAccount,
        isSwap: Boolean = false
    ): Single<String> {
        return Singles.zip(
            ethMemoForHotWalletFeatureFlag.enabled,
            Single.fromObservable(walletApi.walletOptions)
        ).map { (enabled, walletOptions) ->
            if (enabled) {
                val product = resolveProduct(target, isSwap)
                walletOptions.hotWalletAddresses[product.name.lowercase()]?.let { addressesForProduct ->
                    addressesForProduct[getAssetNetworkTicker(sourceCurrency).lowercase()] ?: EMPTY_ADDRESS
                } ?: EMPTY_ADDRESS
            } else {
                EMPTY_ADDRESS
            }
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
        if (currency.isErc20()) {
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
