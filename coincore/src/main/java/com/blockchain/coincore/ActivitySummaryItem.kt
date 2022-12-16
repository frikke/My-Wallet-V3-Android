package com.blockchain.coincore

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.earn.domain.models.interest.InterestState
import com.blockchain.earn.domain.models.staking.StakingState
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.RecurringBuyFailureReason
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Calendar
import kotlin.math.sign

abstract class CryptoActivitySummaryItem : ActivitySummaryItem() {
    abstract val asset: AssetInfo
}

class FiatActivitySummaryItem(
    val currency: FiatCurrency,
    override val exchangeRates: ExchangeRatesDataManager,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: FiatAccount,
    val type: TransactionType,
    val state: TransactionState,
    val paymentMethodId: String?,
) : ActivitySummaryItem() {
    override val stateIsFinalised: Boolean
        get() = state != TransactionState.PENDING

    override fun toString(): String = "currency = $currency " +
        "transactionType  = $type " +
        "timeStamp  = $timeStampMs " +
        "total  = ${value.toStringWithSymbol()} " +
        "txId (hash)  = $txId "
}

abstract class ActivitySummaryItem : Comparable<ActivitySummaryItem> {
    protected abstract val exchangeRates: ExchangeRatesDataManager

    abstract val txId: String
    abstract val timeStampMs: Long
    abstract val stateIsFinalised: Boolean
    abstract val value: Money

    fun fiatValue(selectedFiat: FiatCurrency): Money =
        value.toFiat(selectedFiat, exchangeRates)

    val date: Calendar
        get() = Calendar.getInstance().apply { timeInMillis = timeStampMs }

    final override operator fun compareTo(
        other: ActivitySummaryItem,
    ) = (other.timeStampMs - timeStampMs).sign

    abstract val account: SingleAccount
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
    val state: CustodialOrderState,
    val direction: TransferDirection,
    val receivingValue: Money,
    val depositNetworkFee: Single<Money>,
    val withdrawalNetworkFee: Money,
    val currencyPair: CurrencyPair,
    val fiatValue: Money,
    val fiatCurrency: FiatCurrency,
) : ActivitySummaryItem() {
    override val account: SingleAccount
        get() = sendingAccount

    override val stateIsFinalised: Boolean
        get() = state >= CustodialOrderState.EXPIRED

    override val value: Money
        get() = sendingValue
}

data class RecurringBuyActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val asset: AssetInfo,
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
) : CryptoActivitySummaryItem() {
    override val stateIsFinalised: Boolean
        get() = transactionState > OrderState.PENDING_EXECUTION
}

data class CustodialInterestActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val asset: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: CryptoAccount,
    val status: InterestState,
    val type: TransactionSummary.TransactionType,
    val confirmations: Int,
    val accountRef: String,
    val recipientAddress: String,
    val fiatValue: Money?
) : CryptoActivitySummaryItem() {
    fun isPending(): Boolean =
        status == InterestState.PENDING ||
            status == InterestState.PROCESSING ||
            status == InterestState.MANUAL_REVIEW

    override val stateIsFinalised: Boolean
        get() = status > InterestState.MANUAL_REVIEW
}

data class CustodialStakingActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val asset: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: CryptoAccount,
    val status: StakingState,
    val type: TransactionSummary.TransactionType,
    val confirmations: Int,
    val accountRef: String,
    val recipientAddress: String,
    val fiatValue: Money?
) : CryptoActivitySummaryItem() {
    fun isPending(): Boolean =
        status == StakingState.PENDING ||
            status == StakingState.PROCESSING ||
            status == StakingState.MANUAL_REVIEW

    override val stateIsFinalised: Boolean
        get() = status > StakingState.MANUAL_REVIEW
}

data class CustodialTradingActivitySummaryItem(
    override val exchangeRates: ExchangeRatesDataManager,
    override val asset: AssetInfo,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: CryptoAccount,
    val price: Money?,
    val fundedFiat: Money,
    val status: OrderState,
    val type: OrderType,
    val fee: Money,
    val paymentMethodId: String,
    val paymentMethodType: PaymentMethodType,
    val depositPaymentId: String,
    val recurringBuyId: String? = null,
) : CryptoActivitySummaryItem() {
    override val stateIsFinalised: Boolean
        get() = status > OrderState.PENDING_EXECUTION
}

data class CustodialTransferActivitySummaryItem(
    override val asset: AssetInfo,
    override val exchangeRates: ExchangeRatesDataManager,
    override val txId: String,
    override val timeStampMs: Long,
    override val value: Money,
    override val account: SingleAccount,
    val fee: Money,
    val recipientAddress: String,
    val txHash: String,
    val state: TransactionState,
    val fiatValue: FiatValue,
    val type: TransactionType,
    val paymentMethodId: String?,
) : CryptoActivitySummaryItem() {
    val isConfirmed: Boolean by lazy {
        state == TransactionState.COMPLETED
    }
    override val stateIsFinalised: Boolean
        get() = state != TransactionState.PENDING
}

abstract class NonCustodialActivitySummaryItem : CryptoActivitySummaryItem() {

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

    override fun toString(): String = "cryptoCurrency = $asset" +
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

        return this.asset == that?.asset &&
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
        result = 31 * result + asset.hashCode()
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
        confirmations >= ((asset as? AssetInfo)?.requiredConfirmations ?: return@unsafeLazy false)
    }

    override val stateIsFinalised: Boolean
        get() = isConfirmed
}

typealias ActivitySummaryList = List<ActivitySummaryItem>
