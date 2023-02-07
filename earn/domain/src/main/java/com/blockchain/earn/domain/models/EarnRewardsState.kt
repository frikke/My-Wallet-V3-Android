package com.blockchain.earn.domain.models

import com.blockchain.domain.transactions.CustodialTransactionState

enum class EarnRewardsState : CustodialTransactionState {
    PROCESSING,
    PENDING,
    MANUAL_REVIEW,
    CLEARED,
    REFUNDED,
    FAILED,
    REJECTED,
    COMPLETE,
    UNKNOWN
}
