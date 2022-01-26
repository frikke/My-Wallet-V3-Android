package com.blockchain.coincore.impl

import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.TransactionTarget
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Single

class EthHotWalletAddressResolver(private val hotWalletService: HotWalletService) : AddressResolver {
    override fun getReceiveAddress(
        currency: Currency,
        target: TransactionTarget,
        action: AssetAction
    ): Single<String> {
        val isSwap = action == AssetAction.Swap
        return when (target) {
            is CryptoAccount ->
                hotWalletService.resolveReceiveAddress(
                    sourceCurrency = currency,
                    target = target,
                    isSwap = isSwap
                )
            else -> Single.just("")
        }
    }
}
