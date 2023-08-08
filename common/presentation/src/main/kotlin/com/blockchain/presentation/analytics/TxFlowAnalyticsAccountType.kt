package com.blockchain.presentation.analytics

import com.blockchain.coincore.BankAccount
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TransactionTarget

enum class TxFlowAnalyticsAccountType {
    TRADING, USERKEY, SAVINGS, EXTERNAL;

    companion object {
        fun fromAccount(account: BlockchainAccount): TxFlowAnalyticsAccountType =
            when (account) {
                is TradingAccount,
                is BankAccount -> TRADING

                is EarnRewardsAccount.Interest -> SAVINGS
                else -> USERKEY
            }

        fun fromTransactionTarget(transactionTarget: TransactionTarget): TxFlowAnalyticsAccountType {
            (transactionTarget as? BlockchainAccount)?.let {
                return fromAccount(it)
            } ?: return EXTERNAL
        }
    }
}
