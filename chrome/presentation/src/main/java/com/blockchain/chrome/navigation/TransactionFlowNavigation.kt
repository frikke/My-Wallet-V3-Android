package com.blockchain.chrome.navigation

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.TransactionTarget

interface TransactionFlowNavigation {
    fun startTransactionFlow(
        action: AssetAction,
        origin: String,
        sourceAccount: BlockchainAccount? = null,
        target: TransactionTarget? = null
    )
}
