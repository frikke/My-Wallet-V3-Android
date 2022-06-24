package com.blockchain.coincore.selfcustody

import com.blockchain.api.selfcustody.Status
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialActivitySummaryItem
import com.blockchain.core.chains.dynamicselfcustody.NonCustodialTxHistoryItem
import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class DynamicActivitySummaryItem(
    override val asset: AssetInfo,
    private val event: NonCustodialTxHistoryItem,
    private val accountAddress: String,
    override val exchangeRates: ExchangeRatesDataManager,
    override val account: CryptoAccount
) : NonCustodialActivitySummaryItem() {

    override val transactionType: TransactionSummary.TransactionType by unsafeLazy {
        when {
            event.from == accountAddress -> TransactionSummary.TransactionType.SENT
            event.to == accountAddress -> TransactionSummary.TransactionType.RECEIVED
            else -> TransactionSummary.TransactionType.TRANSFERRED
        }
    }

    override val timeStampMs: Long = event.timestamp * 1000

    override val value: CryptoValue = CryptoValue.fromMinor(asset, event.value)

    override val supportsDescription: Boolean = false

    override val description: String = ""

    override val fee: Observable<Money>
        get() = Observable.just(Money.fromMinor(asset, event.fee.toBigInteger()))

    override val txId: String = event.txId

    override val inputsMap: Map<String, CryptoValue> =
        mapOf(event.from to CryptoValue.fromMinor(asset, event.value))

    override val outputsMap: Map<String, CryptoValue> =
        mapOf(event.to to CryptoValue.fromMinor(asset, event.value))

    // TODO(dtverdota): Use the status instead of number of confirmations with the new API where possible
    override val confirmations: Int = when (event.status) {
        Status.PENDING -> 0
        Status.CONFIRMING -> asset.requiredConfirmations - 1
        Status.COMPLETED,
        Status.FAILED -> asset.requiredConfirmations
    }

    override fun updateDescription(description: String): Completable = Completable.complete()
}
