package com.blockchain.nabu.datamanagers

import com.blockchain.api.NabuApiException
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.domain.paymentmethods.model.CryptoWithdrawalFeeAndLimit
import com.blockchain.domain.paymentmethods.model.FiatWithdrawalFeeAndLimit
import com.blockchain.domain.paymentmethods.model.LegacyLimits
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.repositories.interest.Eligibility
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimits
import com.blockchain.nabu.datamanagers.repositories.swap.TradeTransactionItem
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.nabu.models.responses.simplebuy.BuySellOrderResponse
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyRequestBody
import com.blockchain.nabu.models.responses.simplebuy.TransactionAttributesResponse
import com.blockchain.nabu.models.responses.simplebuy.TransactionResponse
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import java.util.Date

enum class OrderState {
    UNKNOWN,
    UNINITIALISED,
    INITIALISED,
    PENDING_CONFIRMATION, // Has created but not confirmed
    AWAITING_FUNDS, // Waiting for a bank transfer etc
    PENDING_EXECUTION, // Funds received, but crypto not yet released (don't know if we'll need this?)
    FINISHED,
    CANCELED,
    FAILED;

    fun isPending(): Boolean =
        this == PENDING_CONFIRMATION ||
            this == PENDING_EXECUTION ||
            this == AWAITING_FUNDS

    fun hasFailed(): Boolean = this == FAILED

    fun isFinished(): Boolean = this == FINISHED

    fun isCancelled(): Boolean = this == CANCELED
}

interface CustodialWalletManager {
    fun getSupportedBuySellCryptoCurrencies(): Single<List<CurrencyPair>>

    fun getSupportedFiatCurrencies(): Single<List<FiatCurrency>>

    fun fetchFiatWithdrawFeeAndMinLimit(
        fiatCurrency: FiatCurrency,
        product: Product,
        paymentMethodType: PaymentMethodType
    ): Single<FiatWithdrawalFeeAndLimit>

    fun fetchCryptoWithdrawFeeAndMinLimit(
        asset: AssetInfo,
        product: Product
    ): Single<CryptoWithdrawalFeeAndLimit>

    fun fetchWithdrawLocksTime(
        paymentMethodType: PaymentMethodType,
        fiatCurrency: FiatCurrency
    ): Single<BigInteger>

    fun createOrder(
        custodialWalletOrder: CustodialWalletOrder,
        stateAction: String? = null
    ): Single<BuySellOrder>

    fun createRecurringBuyOrder(
        recurringBuyRequestBody: RecurringBuyRequestBody
    ): Single<RecurringBuyOrder>

    fun createWithdrawOrder(
        amount: Money,
        bankId: String
    ): Completable

    fun getCustodialFiatTransactions(
        fiatCurrency: FiatCurrency,
        product: Product,
        type: String? = null
    ): Single<List<FiatTransaction>>

    fun getCustodialCryptoTransactions(
        asset: AssetInfo,
        product: Product,
        type: String? = null
    ): Single<List<CryptoTransaction>>

    fun getBankAccountDetails(
        currency: FiatCurrency
    ): Single<BankAccount>

    fun getCustodialAccountAddress(asset: Currency): Single<String>

    fun isCurrencySupportedForSimpleBuy(
        fiatCurrency: FiatCurrency
    ): Single<Boolean>

    fun isCurrencyAvailableForTrading(
        assetInfo: AssetInfo
    ): Single<Boolean>

    fun isAssetSupportedForSwap(
        assetInfo: AssetInfo
    ): Single<Boolean>

    fun getOutstandingBuyOrders(asset: AssetInfo): Single<BuyOrderList>

    fun getAllOutstandingBuyOrders(): Single<BuyOrderList>

    fun getAllOutstandingOrders(): Single<List<BuySellOrder>>

    fun getAllOrdersFor(asset: AssetInfo): Single<BuyOrderList>

    fun getBuyOrder(orderId: String): Single<BuySellOrder>

    fun deleteBuyOrder(orderId: String): Completable

    fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Single<String>

    // For test/dev
    fun cancelAllPendingOrders(): Completable

    fun getCardAcquirers(): Single<List<PaymentCardAcquirer>>

    fun getBankTransferLimits(
        fiatCurrency: FiatCurrency,
        onlyEligible: Boolean
    ): Single<PaymentLimits>

    fun confirmOrder(
        orderId: String,
        attributes: SimpleBuyConfirmationAttributes?,
        paymentMethodId: String?,
        isBankPartner: Boolean?
    ): Single<BuySellOrder>

    fun getInterestAccountRates(asset: AssetInfo): Single<Double>

    fun getInterestAccountAddress(asset: AssetInfo): Single<String>

    fun getInterestActivity(asset: AssetInfo): Single<List<InterestActivityItem>>

    fun getInterestLimits(asset: AssetInfo): Single<InterestLimits>

    fun getInterestAvailabilityForAsset(asset: AssetInfo): Single<Boolean>

    fun getInterestEnabledAssets(): Single<List<AssetInfo>>

    fun getInterestEligibilityForAsset(asset: AssetInfo): Single<Eligibility>

    fun startInterestWithdrawal(asset: AssetInfo, amount: Money, address: String): Completable

    fun getSupportedFundsFiats(fiatCurrency: FiatCurrency = selectedFiatcurrency): Single<List<FiatCurrency>>

    fun getExchangeSendAddressFor(asset: AssetInfo): Maybe<String>

    fun isSimplifiedDueDiligenceEligible(): Single<Boolean>

    fun fetchSimplifiedDueDiligenceUserState(): Single<SimplifiedDueDiligenceUserState>

    fun createCustodialOrder(
        direction: TransferDirection,
        quoteId: String,
        volume: Money,
        destinationAddress: String? = null,
        refundAddress: String? = null
    ): Single<CustodialOrder>

    fun createPendingDeposit(
        crypto: AssetInfo,
        address: String,
        hash: String,
        amount: Money,
        product: Product
    ): Completable

    fun getProductTransferLimits(
        currency: FiatCurrency,
        product: Product,
        orderDirection: TransferDirection? = null
    ): Single<TransferLimits>

    fun getSwapTrades(): Single<List<CustodialOrder>>

    fun getCustodialActivityForAsset(
        cryptoCurrency: AssetInfo,
        directions: Set<TransferDirection>
    ): Single<List<TradeTransactionItem>>

    fun updateOrder(
        id: String,
        success: Boolean
    ): Completable

    fun isFiatCurrencySupported(destination: String): Boolean

    fun executeCustodialTransfer(amount: Money, origin: Product, destination: Product): Completable

    val selectedFiatcurrency: FiatCurrency

    fun getRecurringBuysForAsset(asset: AssetInfo): Single<List<RecurringBuy>>

    fun getRecurringBuyForId(recurringBuyId: String): Single<RecurringBuy>

    fun cancelRecurringBuy(recurringBuyId: String): Completable
}

data class InterestActivityItem(
    val value: CryptoValue,
    val cryptoCurrency: AssetInfo,
    val id: String,
    val insertedAt: Date,
    val state: InterestState,
    val type: TransactionSummary.TransactionType,
    val extraAttributes: TransactionAttributesResponse?
) {
    companion object {
        fun toInterestState(state: String): InterestState =
            when (state) {
                TransactionResponse.FAILED -> InterestState.FAILED
                TransactionResponse.REJECTED -> InterestState.REJECTED
                TransactionResponse.PROCESSING -> InterestState.PROCESSING
                TransactionResponse.CREATED,
                TransactionResponse.COMPLETE -> InterestState.COMPLETE
                TransactionResponse.PENDING -> InterestState.PENDING
                TransactionResponse.MANUAL_REVIEW -> InterestState.MANUAL_REVIEW
                TransactionResponse.CLEARED -> InterestState.CLEARED
                TransactionResponse.REFUNDED -> InterestState.REFUNDED
                else -> InterestState.UNKNOWN
            }

        fun toTransactionType(type: String) =
            when (type) {
                TransactionResponse.DEPOSIT -> TransactionSummary.TransactionType.DEPOSIT
                TransactionResponse.WITHDRAWAL -> TransactionSummary.TransactionType.WITHDRAW
                TransactionResponse.INTEREST_OUTGOING -> TransactionSummary.TransactionType.INTEREST_EARNED
                else -> TransactionSummary.TransactionType.UNKNOWN
            }
    }
}

enum class InterestState {
    FAILED,
    REJECTED,
    PROCESSING,
    COMPLETE,
    PENDING,
    MANUAL_REVIEW,
    CLEARED,
    REFUNDED,
    UNKNOWN
}

data class InterestAccountDetails(
    val balance: CryptoValue,
    val pendingInterest: CryptoValue,
    val pendingDeposit: CryptoValue,
    val totalInterest: CryptoValue,
    val lockedBalance: CryptoValue
)

data class PaymentAttributes(
    val authorisationUrl: String?,
    val cardAttributes: CardAttributes = CardAttributes.Empty
) {
    val isCardPayment: Boolean by lazy {
        cardAttributes != CardAttributes.Empty
    }
}

enum class CardPaymentState {
    INITIAL, // Should never happen. It means a case was forgotten by backend
    WAITING_FOR_3DS, // We have to display a 3DS verification popup
    CONFIRMED_3DS, // 3DS valid
    SETTLED, // Ready for capture, no need for 3DS
    VOIDED, // Payment voided
    ABANDONED, // Payment abandoned
    FAILED // Payment failed
}

sealed class CardAttributes {

    object Empty : CardAttributes()

    // Very similar to CardProvider, used for BUY
    data class Provider(
        val cardAcquirerName: String,
        val cardAcquirerAccountCode: String,
        val paymentLink: String,
        val paymentState: CardPaymentState,
        val clientSecret: String,
        val publishableApiKey: String
    ) : CardAttributes()

    data class EveryPay(
        val paymentLink: String,
        val paymentState: CardPaymentState
    ) : CardAttributes()
}

data class BuySellOrder(
    val id: String,
    val pair: String,
    val source: Money,
    val target: Money,
    val paymentMethodId: String,
    val paymentMethodType: PaymentMethodType,
    val state: OrderState = OrderState.UNINITIALISED,
    val expires: Date = Date(),
    val updated: Date = Date(),
    val created: Date = Date(),
    val fee: Money? = null,
    val price: Money? = null,
    val orderValue: Money? = null,
    val attributes: PaymentAttributes? = null,
    val type: OrderType,
    val depositPaymentId: String,
    val approvalErrorStatus: ApprovalErrorStatus = ApprovalErrorStatus.None,
    val failureReason: String? = null,
    val paymentError: ApprovalErrorStatus = ApprovalErrorStatus.None,
    val recurringBuyId: String? = null
)

fun String?.toRecurringBuyFailureReason(): RecurringBuyFailureReason? {
    return this?.let {
        when (it) {
            BuySellOrderResponse.FAILED_INSUFFICIENT_FUNDS ->
                RecurringBuyFailureReason.INSUFFICIENT_FUNDS
            BuySellOrderResponse.FAILED_INTERNAL_ERROR ->
                RecurringBuyFailureReason.INTERNAL_SERVER_ERROR
            BuySellOrderResponse.FAILED_BENEFICIARY_BLOCKED ->
                RecurringBuyFailureReason.BLOCKED_BENEFICIARY_ID
            BuySellOrderResponse.FAILED_BAD_FILL ->
                RecurringBuyFailureReason.FAILED_BAD_FILL
            else -> RecurringBuyFailureReason.UNKNOWN
        }
    }
}

enum class RecurringBuyFailureReason {
    INSUFFICIENT_FUNDS,
    BLOCKED_BENEFICIARY_ID,
    INTERNAL_SERVER_ERROR,
    FAILED_BAD_FILL,
    UNKNOWN
}

sealed class ApprovalErrorStatus {
    // Card create errors
    object CardDuplicate : ApprovalErrorStatus()
    object CardCreateFailed : ApprovalErrorStatus()
    object CardCreateAbandoned : ApprovalErrorStatus()
    object CardCreateExpired : ApprovalErrorStatus()
    object CardCreateBankDeclined : ApprovalErrorStatus()
    object CardCreateDebitOnly : ApprovalErrorStatus()
    object CardCreateNoToken : ApprovalErrorStatus()

    // Card payment errors
    object CardPaymentNotSupported : ApprovalErrorStatus()
    object CardPaymentFailed : ApprovalErrorStatus()
    object InsufficientCardFunds : ApprovalErrorStatus()
    object CardPaymentDebitOnly : ApprovalErrorStatus()
    object CardBlockchainDecline : ApprovalErrorStatus()
    object CardAcquirerDecline : ApprovalErrorStatus()

    object Invalid : ApprovalErrorStatus()
    object Failed : ApprovalErrorStatus()
    object Declined : ApprovalErrorStatus()
    object Rejected : ApprovalErrorStatus()
    object Expired : ApprovalErrorStatus()
    object LimitedExceeded : ApprovalErrorStatus()
    object AccountInvalid : ApprovalErrorStatus()
    object FailedInternal : ApprovalErrorStatus()
    object InsufficientFunds : ApprovalErrorStatus()
    object None : ApprovalErrorStatus()
    class Undefined(val error: String) : ApprovalErrorStatus()
}

typealias BuyOrderList = List<BuySellOrder>

@kotlinx.serialization.Serializable
data class OrderInput(
    private val symbol: String,
    private val amount: String? = null
)

@kotlinx.serialization.Serializable
data class OrderOutput(
    private val symbol: String,
    private val amount: String? = null
)

data class FiatTransaction(
    val amount: FiatValue,
    val id: String,
    val date: Date,
    val type: TransactionType,
    val state: TransactionState,
    val paymentId: String?
)

data class CryptoTransaction(
    val amount: Money,
    val id: String,
    val date: Date,
    val type: TransactionType,
    val state: TransactionState,
    val receivingAddress: String,
    val fee: Money,
    val txHash: String,
    val currency: FiatCurrency,
    val paymentId: String?
)

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL
}

enum class TransactionState {
    COMPLETED,
    PENDING,
    FAILED
}

enum class CustodialOrderState {
    CREATED,
    PENDING_CONFIRMATION,
    PENDING_LEDGER,
    CANCELED,
    PENDING_EXECUTION,
    PENDING_DEPOSIT,
    FINISH_DEPOSIT,
    PENDING_WITHDRAWAL,
    EXPIRED,
    FINISHED,
    FAILED,
    UNKNOWN;

    private val pendingState: Set<CustodialOrderState>
        get() = setOf(
            PENDING_EXECUTION,
            PENDING_CONFIRMATION,
            PENDING_LEDGER,
            PENDING_DEPOSIT,
            PENDING_WITHDRAWAL,
            FINISH_DEPOSIT
        )

    val isPending: Boolean
        get() = pendingState.contains(this)

    private val failedState: Set<CustodialOrderState>
        get() = setOf(FAILED)

    val hasFailed: Boolean
        get() = failedState.contains(this)

    val displayableState: Boolean
        get() = isPending || this == FINISHED
}

data class BuySellPair(
    val cryptoCurrency: Currency,
    val fiatCurrency: Currency,
    val buyLimits: BuySellLimits,
    val sellLimits: BuySellLimits
)

data class BuySellLimits(private val min: BigInteger, private val max: BigInteger) {
    fun minLimit(currency: Currency): Money = Money.fromMinor(currency, min)
    fun maxLimit(currency: Currency): Money = Money.fromMinor(currency, max)
}

enum class TransferDirection {
    ON_CHAIN, // from non-custodial to non-custodial
    FROM_USERKEY, // from non-custodial to custodial
    TO_USERKEY, // from custodial to non-custodial - not in use currently
    INTERNAL; // from custodial to custodial
}

data class BankAccount(val details: List<BankDetail>)

data class BankDetail(val title: String, val value: String, val isCopyable: Boolean = false)

data class PaymentCardAcquirer(
    val cardAcquirerName: String,
    val cardAcquirerAccountCodes: List<String>,
    val apiKey: String
)

internal fun String.toSupportedPartner(): Partner =
    when (this) {
        "EVERYPAY" -> Partner.EVERYPAY
        "CARDPROVIDER" -> Partner.CARDPROVIDER
        else -> Partner.UNKNOWN
    }

sealed class TransactionError : Throwable() {
    object OrderLimitReached : TransactionError()
    object OrderNotCancelable : TransactionError()
    object WithdrawalAlreadyPending : TransactionError()
    object WithdrawalBalanceLocked : TransactionError()
    object WithdrawalInsufficientFunds : TransactionError()
    object InternalServerError : TransactionError()
    object TradingTemporarilyDisabled : TransactionError()
    object InsufficientBalance : TransactionError()
    object OrderBelowMin : TransactionError()
    object OrderAboveMax : TransactionError()
    object SwapDailyLimitExceeded : TransactionError()
    object SwapWeeklyLimitExceeded : TransactionError()
    object SwapYearlyLimitExceeded : TransactionError()
    object InvalidCryptoAddress : TransactionError()
    object InvalidDomainAddress : TransactionError()
    object InvalidCryptoCurrency : TransactionError()
    object InvalidFiatCurrency : TransactionError()
    object OrderDirectionDisabled : TransactionError()
    object InvalidOrExpiredQuote : TransactionError()
    object IneligibleForSwap : TransactionError()
    object InvalidDestinationAmount : TransactionError()
    object InvalidPostcode : TransactionError()
    object TransactionDenied : TransactionError()
    object ExecutionFailed : TransactionError()
    object InternetConnectionError : TransactionError()
    class HttpError(val nabuApiException: NabuApiException) : TransactionError()
}

enum class Product {
    BUY,
    SELL,
    SAVINGS,
    TRADE
}

data class TransferQuote(
    val id: String = "",
    val prices: List<PriceTier> = emptyList(),
    val expirationDate: Date = Date(),
    val creationDate: Date = Date(),
    val networkFee: Money,
    val staticFee: Money,
    val sampleDepositAddress: String
)

data class CurrencyPair(val source: Currency, val destination: Currency) {

    val rawValue: String
        get() = listOf(source.networkTicker, destination.networkTicker).joinToString("-")

    companion object {
        fun fromRawPair(
            rawValue: String,
            assetCatalogue: AssetCatalogue
        ): CurrencyPair? {
            val parts = rawValue.split("-")
            val source: Currency = assetCatalogue.fromNetworkTicker(parts[0]) ?: return null
            val destination: Currency = assetCatalogue.fromNetworkTicker(parts[1]) ?: return null
            return CurrencyPair(source, destination)
        }
    }
}

data class PriceTier(
    val volume: Money,
    val price: Money
)

data class TransferLimits(
    val minLimit: Money,
    val maxOrder: Money,
    val maxLimit: Money
) : LegacyLimits {
    constructor(currency: Currency) : this(
        minLimit = Money.zero(currency),
        maxOrder = Money.zero(currency),
        maxLimit = Money.zero(currency)
    )

    override val min: Money
        get() = minLimit
    override val max: Money
        get() = maxLimit
}

data class CustodialOrder(
    val id: String,
    val state: CustodialOrderState,
    val depositAddress: String?,
    val createdAt: Date,
    val inputMoney: Money,
    val outputMoney: Money
)

data class SimplifiedDueDiligenceUserState(
    val isVerified: Boolean,
    val stateFinalised: Boolean
)

data class RecurringBuyOrder(
    val state: RecurringBuyState = RecurringBuyState.UNINITIALISED
)
