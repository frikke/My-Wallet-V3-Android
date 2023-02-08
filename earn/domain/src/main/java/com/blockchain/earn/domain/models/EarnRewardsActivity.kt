package com.blockchain.earn.domain.models

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import java.util.Date

data class EarnRewardsActivity(
    val value: CryptoValue,
    val fiatValue: Money?,
    val id: String,
    val insertedAt: Date,
    val state: EarnRewardsState,
    val type: TransactionSummary.TransactionType,
    val extraAttributes: EarnRewardsActivityAttributes?
)
