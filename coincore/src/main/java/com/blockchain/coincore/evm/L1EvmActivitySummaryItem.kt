package com.blockchain.coincore.evm

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialActivitySummaryItem
import com.blockchain.core.chains.erc20.model.Erc20HistoryEvent
import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import java.math.BigInteger
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class L1EvmActivitySummaryItem(
    override val asset: AssetInfo,
    private val event: Erc20HistoryEvent,
    private val accountHash: String,
    override val exchangeRates: ExchangeRatesDataManager,
    lastBlockNumber: BigInteger,
    override val account: CryptoAccount
) : NonCustodialActivitySummaryItem() {

    override val transactionType: TransactionSummary.TransactionType by unsafeLazy {
        when {
            event.isToAccount(accountHash) &&
                event.isFromAccount(accountHash) -> TransactionSummary.TransactionType.TRANSFERRED
            event.isFromAccount(accountHash) -> TransactionSummary.TransactionType.SENT
            else -> TransactionSummary.TransactionType.RECEIVED
        }
    }

    override val timeStampMs: Long = event.timestamp

    override val value: CryptoValue = event.value

    override val supportsDescription: Boolean
        get() = false

    override val description: String
        get() = ""

    override val fee: Observable<Money>
        get() = event.fee.toObservable()

    override val txId: String = event.transactionHash

    override val inputsMap: Map<String, CryptoValue> =
        mapOf(event.from to event.value)

    override val outputsMap: Map<String, CryptoValue> =
        mapOf(event.to to event.value)

    override val confirmations: Int = (lastBlockNumber - event.blockNumber).toInt()

    override fun updateDescription(description: String): Completable = Completable.complete()
}
