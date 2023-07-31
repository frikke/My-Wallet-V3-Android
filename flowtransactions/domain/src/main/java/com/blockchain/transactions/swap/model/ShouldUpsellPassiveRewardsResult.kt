package com.blockchain.transactions.swap.model

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CustodialInterestAccount

sealed interface ShouldUpsellPassiveRewardsResult {
    object ShouldNot : ShouldUpsellPassiveRewardsResult
    data class ShouldLaunch(
        val sourceAccount: CryptoAccount,
        val targetAccount: CustodialInterestAccount,
        val interestRate: Double
    ) : ShouldUpsellPassiveRewardsResult
}
