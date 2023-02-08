package com.blockchain.nfts.navigation

import androidx.compose.runtime.Stable
import com.blockchain.coincore.CryptoAccount

@Stable
interface NftNavigation {
    fun showReceiveSheet(account: CryptoAccount)
}
