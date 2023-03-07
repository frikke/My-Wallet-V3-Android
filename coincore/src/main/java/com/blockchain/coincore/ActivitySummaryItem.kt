package com.blockchain.coincore

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.transactions.CustodialTransactionState
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.earn.domain.models.EarnRewardsState
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.RecurringBuyFailureReason
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Calendar
import kotlin.math.sign

interface CryptoActivitySummaryItem : ActivitySummaryItem {
    val currency: Currency
}

interface CustodialTransaction : CryptoActivitySummaryItem {
    val state: CustodialTransactionState
}

class FiatActivitySummaryItem(
    override val currency: FiatCurrency,
    override val exchangeRates: ExchangeRatesDataManager,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    val fiat: Money,
    override val account: FiatAccount,
    val type: TransactionType,
    override val state: TransactionState,
    val paymentMethodId: String?,
) : CustodialTransaction {
    override val stateIsFinalised: Boolean
        get() = state.isFinalised

    override fun toString(): String = "currency = $currency " +
        "transactionType  = $type " +
        "timeStamp  = $timeStampMs " +
        "total  = ${value.toStringWithSymbol()} " +
        "txId (hash)  = $txId "
}

interface ActivitySummaryItem : Comparable<ActivitySummaryItem> {
    val exchangeRates: ExchangeRatesDataManager

    val txId: String
    val timeStampMs: Long
    val stateIsFinalised: Boolean
    val value: Money

    val date: Calendar
        get() = Calendar.getInstance().apply { timeInMillis = timeStampMs }

    override operator fun compareTo(
        other: ActivitySummaryItem,
    ) = (other.timeStampMs - timeStampMs).sign

    val account: SingleAccount
}

data class TradeActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val txId: String,
    override val timeStampMs: Long,
    val price: Money? = null,
    val sendingValue: Money,
    val sendingAccount: SingleAccount,
    val sendingAddress: String?,
    val receivingAddress: String?,
    override val state: CustodialOrderState,
    val direction: TransferDirection,
    val receivingValue: Money,
    val depositNetworkFee: Single<Money>,
    val withdrawalNetworkFee: Money,
    val currencyPair: CurrencyPair,
    val fiatValue: Money,
    override val currency: FiatCurrency,
) : CustodialTransaction {
    override val account: SingleAccount
        get() = sendingAccount

    override val stateIsFinalised: Boolean
        get() = state >= CustodialOrderState.EXPIRED

    override val value: Money
        get() = sendingValue
}

data class RecurringBuyActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val currency: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: SingleAccount,
    val fundedFiat: Money,
    val transactionState: OrderState,
    val failureReason: RecurringBuyFailureReason?,
    val fee: Money,
    val paymentMethodId: String,
    val paymentMethodType: PaymentMethodType,
    val type: OrderType,
    val recurringBuyId: String?,
) : CustodialTransaction {
    override val state: CustodialTransactionState
        get() = transactionState
    override val stateIsFinalised: Boolean
        get() = transactionState > OrderState.PENDING_EXECUTION
}

data class CustodialInterestActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val currency: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: CryptoAccount,
    override val state: EarnRewardsState,
    val type: TransactionSummary.TransactionType,
    val confirmations: Int,
    val accountRef: String,
    val recipientAddress: String,
    val fiatValue: Money?
) : CustodialTransaction {
    fun isPending(): Boolean =
        state == EarnRewardsState.PENDING ||
            state == EarnRewardsState.PROCESSING ||
            state == EarnRewardsState.MANUAL_REVIEW

    override val stateIsFinalised: Boolean
        get() = state > EarnRewardsState.MANUAL_REVIEW
}

data class CustodialStakingActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val currency: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: CryptoAccount,
    override val state: EarnRewardsState,
    val type: TransactionSummary.TransactionType,
    val confirmations: Int,
    val accountRef: String,
    val recipientAddress: String,
    val fiatValue: Money?
) : CustodialTransaction {
    fun isPending(): Boolean =
        state == EarnRewardsState.PENDING ||
            state == EarnRewardsState.PROCESSING ||
            state == EarnRewardsState.MANUAL_REVIEW

    override val stateIsFinalised: Boolean
        get() = state > EarnRewardsState.MANUAL_REVIEW
}

data class CustodialActiveRewardsActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val currency: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: CryptoAccount,
    override val state: EarnRewardsState,
    val type: TransactionSummary.TransactionType,
    val confirmations: Int,
    val accountRef: String,
    val recipientAddress: String,
    val fiatValue: Money?
) : CustodialTransaction {
    fun isPending(): Boolean =
        state == EarnRewardsState.PENDING ||
            state == EarnRewardsState.PROCESSING ||
            state == EarnRewardsState.MANUAL_REVIEW

    override val stateIsFinalised: Boolean
        get() = state > EarnRewardsState.MANUAL_REVIEW
}

data class CustodialTradingActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val currency: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: CryptoAccount,
    val price: Money?,
    val fundedFiat: Money,
    override val state: OrderState,
    val type: OrderType,
    val fee: Money,
    val paymentMethodId: String,
    val paymentMethodType: PaymentMethodType,
    val depositPaymentId: String,
    val recurringBuyId: String? = null,
) : CustodialTransaction {
    override val stateIsFinalised: Boolean
        get() = state > OrderState.PENDING_EXECUTION
}

data class CustodialTransferActivitySummaryItem(
    override val currency: AssetInfo,
    override val exchangeRates: ExchangeRatesDataManager,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: SingleAccount,
    val fee: Money,
    val recipientAddress: String,
    val txHash: String,
    override val state: TransactionState,
    val fiatValue: FiatValue,
    val type: TransactionType,
    val paymentMethodId: String?,
) : CustodialTransaction {
    val isConfirmed: Boolean by lazy {
        state == TransactionState.COMPLETED
    }
    override val stateIsFinalised: Boolean
        get() = state.isFinalised
}

abstract class NonCustodialActivitySummaryItem : CryptoActivitySummaryItem {

    abstract val transactionType: TransactionSummary.TransactionType
    abstract val fee: Observable<Money>

    abstract val inputsMap: Map<String, CryptoValue>

    abstract val outputsMap: Map<String, CryptoValue>

    abstract val description: String?
    abstract val supportsDescription: Boolean

    open val confirmations = 0
    open val doubleSpend: Boolean = false
    open val isFeeTransaction = false
    open val isPending: Boolean = false
    open var note: String? = null

    override fun toString(): String = "cryptoCurrency = $currency" +
        "transactionType  = $transactionType " +
        "timeStamp  = $timeStampMs " +
        "total  = ${value.toStringWithSymbol()} " +
        "txId (hash)  = $txId " +
        "inputsMap  = $inputsMap " +
        "outputsMap  = $outputsMap " +
        "confirmations  = $confirmations " +
        "doubleSpend  = $doubleSpend " +
        "isPending  = $isPending " +
        "note = $note"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NonCustodialActivitySummaryItem?

        return this.currency == that?.currency &&
            this.transactionType == that.transactionType &&
            this.timeStampMs == that.timeStampMs &&
            this.value == that.value &&
            this.txId == that.txId &&
            this.inputsMap == that.inputsMap &&
            this.outputsMap == that.outputsMap &&
            this.confirmations == that.confirmations &&
            this.doubleSpend == that.doubleSpend &&
            this.isFeeTransaction == that.isFeeTransaction &&
            this.isPending == that.isPending &&
            this.note == that.note
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + currency.hashCode()
        result = 31 * result + transactionType.hashCode()
        result = 31 * result + timeStampMs.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + txId.hashCode()
        result = 31 * result + inputsMap.hashCode()
        result = 31 * result + outputsMap.hashCode()
        result = 31 * result + confirmations.hashCode()
        result = 31 * result + isFeeTransaction.hashCode()
        result = 31 * result + doubleSpend.hashCode()
        result = 31 * result + (note?.hashCode() ?: 0)
        return result
    }

    open fun updateDescription(description: String): Completable =
        Completable.error(IllegalStateException("Update description not supported"))

    val isConfirmed: Boolean by unsafeLazy {
        confirmations >= ((currency as? AssetInfo)?.requiredConfirmations ?: return@unsafeLazy false)
    }

    override val stateIsFinalised: Boolean
        get() = isConfirmed
}

typealias ActivitySummaryList = List<ActivitySummaryItem>
