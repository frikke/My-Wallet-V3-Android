package com.blockchain.core.custodial.domain.model

import info.blockchain.balance.Money

data class TradingAccountBalance(
    val total: Money,
    val withdrawable: Money,
    val pending: Money,
    val hasTransactions: Boolean = false,
)
