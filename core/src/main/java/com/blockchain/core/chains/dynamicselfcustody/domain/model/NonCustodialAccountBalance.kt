package com.blockchain.core.chains.dynamicselfcustody.domain.model

import java.math.BigDecimal
import java.math.BigInteger

data class NonCustodialAccountBalance(
    val networkTicker: String,
    val amount: BigInteger,
    val pending: BigInteger,
    val price: BigDecimal?
)
