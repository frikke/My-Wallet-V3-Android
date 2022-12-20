package com.blockchain.walletconnect.ui.networks

import com.blockchain.core.chains.ethereum.EthDataManager
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
            EthDataManager.ethChain.chainId,
            CryptoCurrency.ETHER.logo
        )
    }
}
