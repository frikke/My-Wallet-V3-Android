package com.blockchain.blockchaincard.util

import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransactionState
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.getMonthName

class BlockchainCardTransactionUtils {
    companion object {
        fun mergeTransactionList(
            firstList: List<BlockchainCardTransaction>?,
            secondList: List<BlockchainCardTransaction>?
        ): List<BlockchainCardTransaction> {
            val finalPendingTransactions = mutableListOf<BlockchainCardTransaction>()
            firstList?.let { finalPendingTransactions.addAll(it) }
            secondList?.let { finalPendingTransactions.addAll(it) }

            return finalPendingTransactions
        }

        fun mergeGroupedTransactionList(
            firstGroupedList: Map<String?, List<BlockchainCardTransaction>>,
            secondGroupedList: Map<String?, List<BlockchainCardTransaction>>
        ): Map<String?, List<BlockchainCardTransaction>> {
            val mergedKeys = firstGroupedList.keys + secondGroupedList.keys
            return mergedKeys.associateWith {
                val firstList = firstGroupedList[it] ?: mutableListOf()
                val secondList = secondGroupedList[it] ?: mutableListOf()
                firstList + secondList
            }
        }
    }
}

fun List<BlockchainCardTransaction>.getPendingTransactions(): List<BlockchainCardTransaction> = filter {
    it.state == BlockchainCardTransactionState.PENDING
}

fun List<BlockchainCardTransaction>.getCompletedTransactions(): List<BlockchainCardTransaction> = filter {
    it.state != BlockchainCardTransactionState.PENDING
}

fun List<BlockchainCardTransaction>.getCompletedTransactionsGroupedByMonth():
    Map<String?, List<BlockchainCardTransaction>> = getCompletedTransactions().groupBy {
    it.userTransactionTime.fromIso8601ToUtc()?.getMonthName()
}
