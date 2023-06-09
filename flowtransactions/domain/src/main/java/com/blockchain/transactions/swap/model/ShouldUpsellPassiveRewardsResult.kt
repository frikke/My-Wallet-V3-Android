package com.blockchain.transactions.swap.model

import com.blockchain.coincore.impl.CustodialInterestAccount

sealed interface ShouldUpsellPassiveRewardsResult {
    object ShouldNot : ShouldUpsellPassiveRewardsResult
    data class ShouldLaunch(
        val interestAccount: CustodialInterestAccount,
        val interestRate: Double
    ) : ShouldUpsellPassiveRewardsResult
}
