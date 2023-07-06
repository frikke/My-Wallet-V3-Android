package com.blockchain.coincore.impl

import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.Logger
import com.blockchain.nabu.service.NabuService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Single

class EthHotWalletAddressResolver(
    private val hotWalletService: HotWalletService,
    private val nabuService: NabuService,
    private val logger: Logger,
    private val dynamicEthHotWalletAddressFF: FeatureFlag
) : AddressResolver {
    private val ethWalletAddressActions = listOf(AssetAction.Send, AssetAction.Sell, AssetAction.Swap)
    override fun getReceiveAddress(
        currency: Currency,
        target: TransactionTarget,
        action: AssetAction
    ): Single<String> {
        if (action !in ethWalletAddressActions) {
            logger.e("No HotWalletAddress for action $action")
            return Single.just("")
        }

        if (action == AssetAction.Swap && target is NonCustodialAccount) {
            logger.e("No HotWalletAddress for $target --- $action")
            return Single.just("")
        }
        /*
        * when sending to a different non custodial account or to an address
        * */
        if (action == AssetAction.Send && (target is NonCustodialAccount || target is CryptoAddress)) {
            logger.e("No HotWalletAddress for $target --- $action")
            return Single.just("")
        }

        return dynamicEthHotWalletAddressFF.enabled.flatMap {
            if (it) {
                handleEnabledDynamicEthWalletAddress(action, currency, target)
            } else {
                handleStaticEthWalletAddress(action, currency, target)
            }
        }
    }

    private fun handleEnabledDynamicEthWalletAddress(
        action: AssetAction,
        currency: Currency,
        target: TransactionTarget
    ): Single<String> {
        val product = when (action) {
            AssetAction.Swap,
            AssetAction.Sell -> "swap"
            AssetAction.Send -> "simplebuy"
            else -> throw UnsupportedOperationException()
        }
        val networkTicker = (currency as AssetInfo).coinNetwork?.networkTicker ?: currency.networkTicker
        return nabuService.getCustodialAccountDetails(product, networkTicker).flatMap { response ->
            val address = response.agent.address
            if (address != null) {
                Single.just(address)
            } else {
                handleStaticEthWalletAddress(action, currency, target)
            }
        }
    }

    private fun handleStaticEthWalletAddress(
        action: AssetAction,
        currency: Currency,
        target: TransactionTarget
    ): Single<String> {
        val isSwap = action == AssetAction.Swap
        return hotWalletService.resolveReceiveAddress(
            sourceCurrency = currency,
            target = target,
            isSwap = isSwap
        )
    }
}
