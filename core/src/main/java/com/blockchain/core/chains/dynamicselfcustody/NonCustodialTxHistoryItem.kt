package com.blockchain.core.chains.dynamicselfcustody

import com.blockchain.api.selfcustody.Status
import java.math.BigInteger

data class NonCustodialTxHistoryItem(
    val txId: String,
    val status: Status,
    val timestamp: Long,
    val fee: String,
    val value: BigInteger,
    val from: String,
    val to: String
)
