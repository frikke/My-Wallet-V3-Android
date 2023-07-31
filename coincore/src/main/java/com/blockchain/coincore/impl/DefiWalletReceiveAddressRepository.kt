package com.blockchain.coincore.impl

import com.blockchain.DefiWalletReceiveAddressService
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.outcome.Outcome
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo

internal class DefiWalletReceiveAddressRepository(
    private val coincore: Coincore,
    private val assetCatalogue: AssetCatalogue
) : DefiWalletReceiveAddressService {
    override suspend fun receiveAddress(assetInfo: AssetInfo): Outcome<Exception, ReceiveAddress> {
        val nativeAssetTicker = assetInfo.coinNetwork?.nativeAssetTicker ?: assetInfo.networkTicker
        val currency =
            assetCatalogue.fromNetworkTicker(nativeAssetTicker) ?: throw IllegalStateException("Unknown currency")
        return coincore[currency].defaultAccount(AssetFilter.NonCustodial).flatMap { acc ->
            acc.receiveAddress
        }.awaitOutcome()
    }
}
