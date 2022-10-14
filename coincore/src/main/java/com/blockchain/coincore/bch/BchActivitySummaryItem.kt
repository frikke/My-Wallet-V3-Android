package com.blockchain.coincore.bch

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialActivitySummaryItem
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

/*internal*/ class BchActivitySummaryItem internal constructor(
    private val transactionSummary: TransactionSummary,
    override val exchangeRates: ExchangeRatesDataManager,
    override val account: CryptoAccount,
    private val payloadDataManager: PayloadDataManager
) : NonCustodialActivitySummaryItem() {

    override val asset = CryptoCurrency.BCH
    override val transactionType: TransactionSummary.TransactionType = transactionSummary.transactionType
    override val timeStampMs: Long = transactionSummary.time * 1000

    override val value: Money = CryptoValue.fromMinor(CryptoCurrency.BCH, transactionSummary.total)

    override val description: String?
        get() = payloadDataManager.getTransactionNotes(txId)

    override val fee: Observable<Money>
        get() = Observable.just(CryptoValue.fromMinor(CryptoCurrency.BCH, transactionSummary.fee))

    override val txId: String =
        transactionSummary.hash

    override val inputsMap: Map<String, CryptoValue>
        get() = transactionSummary.inputsMap.mapValues { CryptoValue.fromMinor(CryptoCurrency.BCH, it.value) }

    override val outputsMap: Map<String, CryptoValue>
        get() = transactionSummary.outputsMap.mapValues { CryptoValue.fromMinor(CryptoCurrency.BCH, it.value) }

    override val confirmations: Int
        get() = transactionSummary.confirmations

    override val doubleSpend: Boolean
        get() = transactionSummary.isDoubleSpend

    override val isPending: Boolean
        get() = transactionSummary.isPending

    override val supportsDescription: Boolean
        get() = true

    override fun updateDescription(description: String): Completable =
        payloadDataManager.updateTransactionNotes(txId, description)
}
