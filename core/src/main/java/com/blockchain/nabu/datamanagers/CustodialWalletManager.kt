package com.blockchain.nabu.datamanagers

import com.blockchain.api.NabuApiException
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.common.model.Seconds
import com.blockchain.domain.paymentmethods.model.CryptoWithdrawalFeeAndLimit
import com.blockchain.domain.paymentmethods.model.FiatWithdrawalFeeAndLimit
import com.blockchain.domain.paymentmethods.model.LegacyLimits
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.transactions.CustodialTransactionState
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.domain.wiretransfer.WireTransferDetails
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.repositories.swap.TradeTransactionItem
import com.blockchain.nabu.models.responses.simplebuy.BuySellOrderResponse
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import java.util.Date
import kotlinx.coroutines.flow.Flow

enum class OrderState : CustodialTransactionState {
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
    @Deprecated("use flow SimpleBuyService::getSupportedBuySellCryptoCurrencies")
    fun getSupportedBuySellCryptoCurrencies(): Single<List<CurrencyPair>>

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

    fun createWithdrawOrder(
        amount: Money,
        bankId: String
    ): Completable

    fun getCustodialCryptoTransactions(
        freshnessStrategy: FreshnessStrategy,
        asset: AssetInfo,
        product: Product,
        type: String? = null
    ): Observable<List<CryptoTransaction>>

    fun getWireTransferDetails(
        currency: FiatCurrency
    ): Single<WireTransferDetails>

    fun getCustodialAccountAddress(product: Product, asset: Currency): Single<String>

    @Deprecated("use flow isCurrencyAvailableForTrading - remove when CoinView is migrated")
    fun isCurrencyAvailableForTradingLegacy(
        assetInfo: AssetInfo
    ): Single<Boolean>

    fun isCurrencyAvailableForTrading(
        assetInfo: AssetInfo,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<DataResource<Boolean>>

    fun availableFiatCurrenciesForTrading(assetInfo: AssetInfo): Single<List<FiatCurrency>>

    fun isAssetSupportedForSwap(
        assetInfo: AssetInfo
    ): Single<Boolean>

    fun getOutstandingBuyOrders(asset: AssetInfo): Single<BuyOrderList>

    fun getAllOutstandingBuyOrders(): Single<BuyOrderList>

    fun getAllOutstandingOrders(): Single<List<BuySellOrder>>

    fun getAllOrdersFor(freshnessStrategy: FreshnessStrategy, asset: AssetInfo): Observable<BuyOrderList>

    fun getBuyOrder(orderId: String): Single<BuySellOrder>

    fun deleteBuyOrder(orderId: String): Completable

    fun transferFundsToWallet(amount: Money, fee: Money, walletAddress: String): Single<String>

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

    fun getSupportedFundsFiats(
        fiatCurrency: FiatCurrency = selectedFiatcurrency,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.ForceRefresh
        )
    ): Flow<List<FiatCurrency>>

    fun getExchangeSendAddressFor(asset: AssetInfo): Maybe<String>

    fun createCustodialOrder(
        direction: TransferDirection,
        quoteId: String,
        volume: Money,
        destinationAddress: String? = null,
        refundAddress: String? = null
    ): Single<CustodialOrder>

    fun pollForCustodialOrderCompletion(
        orderId: String,
        pollEvery: Seconds = 1,
        pollTimes: Int = 5,
    ): Single<Boolean>

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
        directions: Set<TransferDirection>,
        freshnessStrategy: FreshnessStrategy
    ): Observable<List<TradeTransactionItem>>

    fun updateOrder(
        id: String,
        success: Boolean
    ): Completable

    fun executeCustodialTransfer(amount: Money, origin: Product, destination: Product): Completable

    val selectedFiatcurrency: FiatCurrency
}

data class PaymentAttributes(
    val paymentId: String?,
    val authorisationUrl: String?,
    val cardAttributes: CardAttributes = CardAttributes.Empty,
    val needCvv: Boolean = false
) {
    val isCardPayment: Boolean by lazy {
        cardAttributes != CardAttributes.Empty
    }

    val cardPaymentState: CardPaymentState? by lazy {
        when (cardAttributes) {
            is CardAttributes.Provider -> cardAttributes.paymentState
            is CardAttributes.EveryPay -> cardAttributes.paymentState
            is CardAttributes.Empty -> null
        }
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

enum class TransactionState : CustodialTransactionState {
    COMPLETED,
    PENDING,
    MANUAL_REVIEW,
    FAILED;

    val isFinalised: Boolean
        get() = when (this) {
            COMPLETED,
            FAILED -> true
            PENDING,
            MANUAL_REVIEW -> false
        }
}

enum class CustodialOrderState : CustodialTransactionState {
    CREATED,
    PENDING_CONFIRMATION,
    PENDING_LEDGER,
    PENDING_EXECUTION,
    PENDING_DEPOSIT,
    FINISH_DEPOSIT,
    PENDING_WITHDRAWAL,
    EXPIRED,
    CANCELED,
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

data class BankAccount(val details: List<BankDetail>)

data class BankDetail(
    val title: String,
    val value: String,
    val isCopyable: Boolean = false,
    val tooltip: String? = null
)

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

sealed class TransactionError : Exception() {
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
    class FiatDepositError(val errorCode: String) : TransactionError()
    object SettlementInsufficientBalance : TransactionError()
    object SettlementStaleBalance : TransactionError()
    object SettlementGenericError : TransactionError()
    class SettlementRefreshRequired(val accountId: String) : TransactionError()
    class HttpError(val nabuApiException: NabuApiException) : TransactionError()
}

enum class Product {
    BUY,
    SELL,
    SAVINGS,
    TRADE,
    STAKING,
    EARN_CC1W
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
