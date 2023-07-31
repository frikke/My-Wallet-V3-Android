package com.blockchain

import com.blockchain.coincore.ReceiveAddress
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo

interface DefiWalletReceiveAddressService {
    suspend fun receiveAddress(assetInfo: AssetInfo): Outcome<Exception, ReceiveAddress>
}
