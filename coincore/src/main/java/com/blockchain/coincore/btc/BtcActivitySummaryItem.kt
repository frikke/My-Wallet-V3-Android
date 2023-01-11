package com.blockchain.coincore.btc

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialActivitySummaryItem
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

/*internal*/ class BtcActivitySummaryItem internal constructor(
    private val transactionSummary: TransactionSummary,
    private val payloadDataManager: PayloadDataManager,
    override val exchangeRates: ExchangeRatesDataManager,
    override val account: CryptoAccount
) : NonCustodialActivitySummaryItem() {

    override val currency = CryptoCurrency.BTC

    override val transactionType: TransactionSummary.TransactionType
        get() = transactionSummary.transactionType

    override val timeStampMs = transactionSummary.time * 1000

    override val value: CryptoValue by unsafeLazy {
        CryptoValue.fromMinor(CryptoCurrency.BTC, transactionSummary.total)
    }

    override val description: String?
        get() = payloadDataManager.getTransactionNotes(txId)

    override val supportsDescription: Boolean
        get() = true

    override val fee: Observable<Money>
        get() = Observable.just(CryptoValue.fromMinor(CryptoCurrency.BTC, transactionSummary.fee))

    override val txId: String
        get() = transactionSummary.hash

    override val inputsMap: Map<String, CryptoValue>
        get() = transactionSummary.inputsMap
            .mapValues {
                CryptoValue.fromMinor(CryptoCurrency.BTC, it.value)
            }

    override val outputsMap: Map<String, CryptoValue>
        get() = transactionSummary.outputsMap
            .mapValues {
                CryptoValue.fromMinor(CryptoCurrency.BTC, it.value)
            }

    override val confirmations: Int
        get() = transactionSummary.confirmations

    override val doubleSpend: Boolean
        get() = transactionSummary.isDoubleSpend

    override val isPending: Boolean
        get() = transactionSummary.isPending

    override fun updateDescription(description: String): Completable =
        payloadDataManager.updateTransactionNotes(txId, description)
}
