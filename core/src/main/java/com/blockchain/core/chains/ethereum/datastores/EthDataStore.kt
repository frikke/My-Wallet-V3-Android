package com.blockchain.core.chains.ethereum.datastores

import com.blockchain.core.chains.datastores.SimpleDataStore
import com.blockchain.core.chains.ethereum.models.CombinedEthModel
import info.blockchain.wallet.ethereum.EthereumWallet

/**
 * A simple data store class to cache the Ethereum Wallet
 */
class EthDataStore : SimpleDataStore {

    var ethWallet: EthereumWallet? = null
    var ethAddressResponse: CombinedEthModel? = null

    override fun clearData() {
        ethWallet = null
        ethAddressResponse = null
    }
}
