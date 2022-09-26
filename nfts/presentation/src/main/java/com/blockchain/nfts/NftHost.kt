package com.blockchain.nfts

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount

interface NftHost {
    fun showReceiveSheet(account: BlockchainAccount)
    fun showNftDetail(nftId: String)
}