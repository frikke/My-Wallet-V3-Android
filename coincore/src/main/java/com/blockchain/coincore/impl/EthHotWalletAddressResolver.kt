package com.blockchain.coincore.impl

import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.service.NabuService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.utils.awaitOutcome
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Single

class EthHotWalletAddressResolver(
    private val hotWalletService: HotWalletService,
    private val nabuService: NabuService,
    private val dynamicEthHotWalletAddressFF: FeatureFlag,
) : AddressResolver {
    override fun getReceiveAddress(
        currency: Currency,
        target: TransactionTarget,
        action: AssetAction
    ): Single<String> {
        return rxSingleOutcome {
            if (target !is CryptoAccount) return@rxSingleOutcome Outcome.Success("")

            if (dynamicEthHotWalletAddressFF.coEnabled()) {
                val product = when (action) {
                    AssetAction.Swap,
                    AssetAction.Sell -> "swap"
                    AssetAction.Send -> "simplebuy"
                    else -> throw UnsupportedOperationException()
                }
                val networkTicker = (currency as AssetInfo).coinNetwork?.networkTicker ?: currency.networkTicker
                nabuService.getCustodialAccountDetails(product, networkTicker)
                    .awaitOutcome()
                    .flatMap { response ->
                        val address = response.agent.address
                        if (address != null) {
                            Outcome.Success(address)
                        } else {
                            val isSwap = action == AssetAction.Swap
                            hotWalletService.resolveReceiveAddress(
                                sourceCurrency = currency,
                                target = target,
                                isSwap = isSwap
                            ).awaitOutcome()
                        }
                    }
            } else {
                val isSwap = action == AssetAction.Swap
                hotWalletService.resolveReceiveAddress(
                    sourceCurrency = currency,
                    target = target,
                    isSwap = isSwap
                ).awaitOutcome()
            }
        }
    }
}
