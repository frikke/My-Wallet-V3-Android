package com.blockchain.coincore.impl.txEngine

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.TransactionTarget

class MissingLimitsException(action: AssetAction, source: BlockchainAccount, target: TransactionTarget) :
    IllegalStateException("Missing limits for $action from ${source::class.java} to ${target::class.java}")
