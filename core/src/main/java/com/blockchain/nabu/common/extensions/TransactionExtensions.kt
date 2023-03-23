package com.blockchain.nabu.common.extensions

import com.blockchain.nabu.models.responses.simplebuy.TransactionResponse
import info.blockchain.wallet.multiaddress.TransactionSummary

fun String.toTransactionType() =
    when (this) {
        TransactionResponse.DEPOSIT -> TransactionSummary.TransactionType.DEPOSIT
        TransactionResponse.WITHDRAWAL -> TransactionSummary.TransactionType.WITHDRAW
        TransactionResponse.INTEREST_OUTGOING -> TransactionSummary.TransactionType.INTEREST_EARNED
        TransactionResponse.DEBIT -> TransactionSummary.TransactionType.DEBIT
        else -> TransactionSummary.TransactionType.UNKNOWN
    }
