package com.blockchain.walletconnect.ui.networks

import info.blockchain.balance.CryptoCurrency
import java.io.Serializable

data class NetworkInfo(
    val networkTicker: String,
    val name: String,
    val chainId: Int,
    val logo: String? = null
) : Serializable {
    companion object {
        val defaultEvmNetworkInfo = NetworkInfo(
            CryptoCurrency.ETHER.networkTicker,
            CryptoCurrency.ETHER.name,
            ETH_CHAIN_ID,
            CryptoCurrency.ETHER.logo
        )
    }
}

const val ETH_CHAIN_ID = 1
