package com.blockchain.coincore.xlm

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialActivitySummaryItem
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.sunriver.models.XlmTransaction
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

class XlmActivitySummaryItem(
    private val xlmTransaction: XlmTransaction,
    override val exchangeRates: ExchangeRatesDataManager,
    override val account: CryptoAccount,
    private val payloadDataManager: PayloadDataManager
) : NonCustodialActivitySummaryItem() {
    override val currency = CryptoCurrency.XLM

    override val transactionType: TransactionSummary.TransactionType
        get() = if (xlmTransaction.value > CryptoValue.zero(CryptoCurrency.XLM)) {
            TransactionSummary.TransactionType.RECEIVED
        } else {
            TransactionSummary.TransactionType.SENT
        }

    override val timeStampMs: Long
        get() = xlmTransaction.timeStamp.fromIso8601ToUtc()?.toLocalTime()?.time ?: throw IllegalStateException(
            "xlm timeStamp not found"
        )

    override val value: CryptoValue by unsafeLazy {
        xlmTransaction.accountDelta.abs()
    }

    override val description: String?
        get() = payloadDataManager.getTransactionNotes(txId)

    override val fee: Observable<Money>
        get() = Observable.just(xlmTransaction.fee)

    override val txId: String
        get() = xlmTransaction.hash

    override val inputsMap: Map<String, CryptoValue>
        get() = hashMapOf(xlmTransaction.from.accountId to CryptoValue.zero(CryptoCurrency.XLM))

    override val outputsMap: Map<String, CryptoValue>
        get() = hashMapOf(
            xlmTransaction.to.accountId to value
        )

    override val confirmations: Int
        get() = CryptoCurrency.XLM.requiredConfirmations

    val xlmMemo: String
        get() = xlmTransaction.memo.value

    override val supportsDescription: Boolean
        get() = true

    override fun updateDescription(description: String): Completable =
        payloadDataManager.updateTransactionNotes(txId, description)
}
