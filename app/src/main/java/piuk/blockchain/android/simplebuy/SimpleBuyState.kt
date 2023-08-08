package piuk.blockchain.android.simplebuy

import com.blockchain.api.NabuApiException
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.fiat.isOpenBankingCurrency
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.custodial.models.Availability
import com.blockchain.core.custodial.models.BrokerageQuote
import com.blockchain.core.custodial.models.Promo
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.recurringbuy.domain.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyFrequency
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyState
import com.blockchain.domain.common.model.Millis
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.domain.paymentmethods.model.DepositTerms
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.SettlementReason
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.payments.googlepay.manager.request.BillingAddressParameters
import com.blockchain.presentation.complexcomponents.QuickFillButtonData
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.io.Serializable
import java.math.BigInteger
import kotlin.math.floor
import kotlinx.serialization.Contextual
import kotlinx.serialization.Transient
import piuk.blockchain.android.cards.CardAcquirerCredentials
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionFlowStateInfo

/**
 * This is an object that gets serialized with Json so any properties that we don't
 * want to get serialized should be tagged as @Transient
 *
 */
@kotlinx.serialization.Serializable
data class SimpleBuyState constructor(
    val id: String? = null,
    val fiatCurrency: FiatCurrency = FiatCurrency.fromCurrencyCode("USD"),
    override val amount: FiatValue = FiatValue.zero(fiatCurrency),
    val selectedCryptoAsset: @Contextual AssetInfo? = null,
    val orderState: OrderState = OrderState.UNINITIALISED,
    val failureReason: String? = null,
    val kycStartedButNotCompleted: Boolean = false,
    val kycVerificationState: KycState? = null,
    val currentScreen: FlowScreen = FlowScreen.ENTER_AMOUNT,
    val selectedPaymentMethod: SelectedPaymentMethod? = null,
    val quote: BuyQuote? = null,
    val hasQuoteChanged: Boolean = false,
    val orderValue: CryptoValue? = null,
    val paymentSucceeded: Boolean = false,
    val withdrawalLockPeriod: @Contextual BigInteger = BigInteger.ZERO,
    val recurringBuyFrequency: RecurringBuyFrequency = RecurringBuyFrequency.ONE_TIME,
    val suggestedRecurringBuyExperiment: RecurringBuyFrequency = RecurringBuyFrequency.ONE_TIME,
    val recurringBuyId: String? = null,
    val recurringBuyState: RecurringBuyState = RecurringBuyState.UNINITIALISED,
    val eligibleAndNextPaymentRecurringBuy: List<EligibleAndNextPaymentRecurringBuy> = emptyList(),
    val isRecurringBuyToggled: Boolean = false,
    val googlePayDetails: GooglePayDetails? = null,
    val featureFlagSet: FeatureFlagsSet = FeatureFlagsSet(),
    val quotePrice: QuotePrice? = null,
    val hasSeenRecurringBuyOptions: Boolean = true,
    @Transient val shouldUpsellAnotherAsset: Boolean = false,
    @Transient val quickFillButtonData: QuickFillButtonData? = null,
    @Transient val safeConnectTosLink: String? = null,
    @Transient val paymentOptions: PaymentOptions = PaymentOptions(),
    @Transient override val errorState: TransactionErrorState = TransactionErrorState.NONE,
    @Transient val buyErrorState: ErrorState? = null,
    @Transient val quoteError: Exception? = null,
    @Transient override val fiatRate: ExchangeRate? = null,
    @Transient val isLoading: Boolean = false,
    @Transient val cardAcquirerCredentials: CardAcquirerCredentials? = null,
    @Transient val authorisePaymentUrl: String? = null,
    @Transient val linkedBank: LinkedBank? = null,
    @Transient val shouldShowUnlockHigherFunds: Boolean = false,
    @Transient val linkBankTransfer: LinkBankTransfer? = null,
    @Transient val paymentPending: Boolean = false,
    @Transient val paymentFailed: Boolean = false,
    @Transient private val transferLimits: TxLimits? = null,
    @Transient override val transactionsLimit: TransactionsLimit? = null,
    // we use this flag to avoid navigating back and forth, reset after navigating
    @Transient val confirmationActionRequested: Boolean = false,
    @Transient val newPaymentMethodToBeAdded: PaymentMethod? = null,
    @Transient val showAppRating: Boolean = false,
    @Transient val orderFinishedSuccessfullyHandled: Boolean = false,
    @Transient val hasHandled3ds: Boolean = false,
    @Transient val openCvvInput: Boolean = false,
    @Transient val hasHandledCvv: Boolean = false,
    @Transient val securityCodePaymentId: String? = null,
    @Transient val hasAmountComeFromDeeplink: Boolean = false,
    @Transient val promptRecurringBuyIntervals: Boolean = false
) : MviState, TransactionFlowStateInfo {

    val order: SimpleBuyOrder by unsafeLazy {
        SimpleBuyOrder(
            orderState,
            amount
        )
    }

    val recurringBuyEligiblePaymentMethods: List<PaymentMethodType> by lazy {
        eligibleAndNextPaymentRecurringBuy.flatMap { it.eligibleMethods }
            .distinct()
    }

    val selectedPaymentMethodDetails: PaymentMethod? by unsafeLazy {
        selectedPaymentMethod?.id?.let { id ->
            paymentOptions.availablePaymentMethods.firstOrNull { it.id == id }
        }
    }

    val selectedPaymentMethodLimits: TxLimits by unsafeLazy {
        selectedPaymentMethodDetails?.let {
            TxLimits.fromAmounts(min = it.limits.min, max = it.limits.max)
        } ?: TxLimits.withMinAndUnlimitedMax(FiatValue.zero(fiatCurrency))
    }

    val coinHasZeroMargin: Boolean by unsafeLazy {
        quote?.quoteMargin == 0.toDouble()
    }

    val buyOrderLimits: TxLimits by unsafeLazy {
        transferLimits ?: TxLimits.withMinAndUnlimitedMax(FiatValue.zero(fiatCurrency))
    }

    val exchangeRate: Money? by unsafeLazy {
        quote?.price
    }

    override val limits: TxLimits
        get() = buyOrderLimits.combineWith(selectedPaymentMethodLimits)

    fun isSelectedPaymentMethodRecurringBuyEligible(): Boolean =
        when (selectedPaymentMethodDetails) {
            is PaymentMethod.Funds -> recurringBuyEligiblePaymentMethods.contains(PaymentMethodType.FUNDS)
            is PaymentMethod.Bank -> recurringBuyEligiblePaymentMethods.contains(PaymentMethodType.BANK_TRANSFER)
            is PaymentMethod.Card -> recurringBuyEligiblePaymentMethods.contains(PaymentMethodType.PAYMENT_CARD)
            else -> false
        }

    fun isSelectedPaymentMethodEligibleForSelectedFrequency(): Boolean =
        selectedPaymentMethod?.paymentMethodType?.let { paymentMethodType ->
            val eligible =
                eligibleAndNextPaymentRecurringBuy.firstOrNull { it.frequency == recurringBuyFrequency } ?: return false
            eligible.eligibleMethods.contains(paymentMethodType)
        } ?: false

    fun shouldLaunchExternalFlow(): Boolean =
        authorisePaymentUrl != null && linkedBank != null && id != null

    fun isOpenBankingTransfer() = selectedPaymentMethod?.isBank() == true && fiatCurrency.isOpenBankingCurrency()

    fun isAchTransfer() = selectedPaymentMethod?.isBank() == true && fiatCurrency.networkTicker.equals("USD", true)

    override val action: AssetAction
        get() = AssetAction.Buy

    override val sourceAccountType: AssetCategory
        get() = if (selectedPaymentMethod?.paymentMethodType == PaymentMethodType.FUNDS) {
            AssetCategory.TRADING
        } else {
            AssetCategory.NON_CUSTODIAL
        }

    override val sendingAsset: AssetInfo?
        get() = null

    override val receivingAsset: Currency
        get() = selectedCryptoAsset ?: throw IllegalStateException("Receiving asset is empty")

    override val availableBalance: Money?
        get() = selectedPaymentMethodDetails?.availableBalance

    fun shouldRequestNewQuote(lastState: SimpleBuyState?): Boolean {
        return this.featureFlagSet.feynmanEnterAmountFF &&
            (
                (
                    this.amount.isPositive &&
                        lastState?.selectedPaymentMethod != null &&
                        lastState.selectedPaymentMethod != this.selectedPaymentMethod
                    ) ||
                    (lastState?.amount != null && lastState.amount != this.amount)
                )
    }

    fun shouldUpdateNewQuote(lastState: SimpleBuyState?): Boolean {
        return this.featureFlagSet.feynmanEnterAmountFF &&
            this.quotePrice != null &&
            lastState?.quotePrice?.amountInCrypto != this.quotePrice.amountInCrypto
    }
}

enum class KycState {
    /** Docs submitted for Gold and state is pending. Or kyc backend query returned an error  */
    PENDING,

    /** Docs processed, failed kyc. Not error state. */
    FAILED,

    /** Docs processed under manual review */
    IN_REVIEW,

    /** Docs submitted, no result know from server yet */
    UNDECIDED,

    /** Docs uploaded, processed and kyc passed. Eligible for simple buy */
    VERIFIED_AND_ELIGIBLE,

    /** Docs uploaded, processed and kyc passed. User is NOT eligible for simple buy. */
    VERIFIED_BUT_NOT_ELIGIBLE;

    fun verified() = this == VERIFIED_AND_ELIGIBLE || this == VERIFIED_BUT_NOT_ELIGIBLE
}

@kotlinx.serialization.Serializable
data class QuotePrice(
    val amountInCrypto: @Contextual CryptoValue? = null,
    val fee: FiatValue? = null,
    val fiatPrice: FiatValue? = null
)

@kotlinx.serialization.Serializable
data class FeatureFlagsSet(
    val buyQuoteRefreshFF: Boolean = false,
    val plaidFF: Boolean = false,
    val rbExperimentFF: Boolean = false,
    val feynmanEnterAmountFF: Boolean = false,
    val feynmanCheckoutFF: Boolean = false,
    val improvedPaymentUxFF: Boolean = false
)

enum class FlowScreen {
    ENTER_AMOUNT, KYC, KYC_VERIFICATION
}

sealed class ErrorState : Serializable {
    object BankLinkingTimeout : ErrorState()
    object LinkedBankNotSupported : ErrorState()
    data class BankLinkMaxAccountsReached(val error: NabuApiException) : ErrorState()
    data class BankLinkMaxAttemptsReached(val error: NabuApiException) : ErrorState()
    object ApproveBankInvalid : ErrorState()
    object ApprovedBankFailed : ErrorState()
    object ApprovedBankDeclined : ErrorState()
    object ApprovedBankRejected : ErrorState()
    object ApprovedBankExpired : ErrorState()
    object ApprovedBankLimitedExceed : ErrorState()
    object ApprovedBankAccountInvalid : ErrorState()
    object ApprovedBankFailedInternal : ErrorState()
    data class ApprovedBankUndefinedError(val error: String) : ErrorState()
    object ApprovedBankInsufficientFunds : ErrorState()
    object DailyLimitExceeded : ErrorState()
    object WeeklyLimitExceeded : ErrorState()
    object YearlyLimitExceeded : ErrorState()
    object ExistingPendingOrder : ErrorState()
    object InsufficientCardFunds : ErrorState()
    object CardBankDeclined : ErrorState()
    object CardDuplicated : ErrorState()
    object CardBlockchainDeclined : ErrorState()
    object CardAcquirerDeclined : ErrorState()
    object CardPaymentNotSupported : ErrorState()
    object CardCreateFailed : ErrorState()
    object CardPaymentFailed : ErrorState()
    object CardCreateAbandoned : ErrorState()
    object CardCreateExpired : ErrorState()
    object CardCreateBankDeclined : ErrorState()
    object CardCreateDebitOnly : ErrorState()
    object CardPaymentDebitOnly : ErrorState()
    object CardNoToken : ErrorState()
    object ProviderIsNotSupported : ErrorState()
    object Card3DsFailed : ErrorState()
    object UnknownCardProvider : ErrorState()
    data class PaymentFailedError(val error: String) : ErrorState()
    data class UnhandledHttpError(val nabuApiException: NabuApiException) : ErrorState()
    object InternetConnectionError : ErrorState()
    object BuyPaymentMethodsUnavailable : ErrorState()
    class SettlementRefreshRequired(val accountId: String) : ErrorState()
    object SettlementInsufficientBalance : ErrorState()
    object SettlementStaleBalance : ErrorState()
    object SettlementGenericError : ErrorState()
    class ServerSideUxError(val serverSideUxErrorInfo: ServerSideUxErrorInfo) : ErrorState()
}

data class SimpleBuyOrder(
    val orderState: OrderState = OrderState.UNINITIALISED,
    val amount: FiatValue? = null
)

data class PaymentOptions(
    val availablePaymentMethods: List<PaymentMethod> = emptyList()
)

@kotlinx.serialization.Serializable
data class BuyQuote(
    val id: String? = null,
    val price: FiatValue,
    val availability: Availability? = null,
    val settlementReason: SettlementReason? = null,
    val quoteMargin: Double? = null,
    val feeDetails: BuyFees,
    val createdAt: Millis,
    val expiresAt: Millis,
    val remainingTime: Long,
    val chunksTimeCounter: MutableList<Int> = mutableListOf(),
    val depositTerms: DepositTerms?
) {

    companion object {
        private const val MIN_QUOTE_REFRESH = 90L

        fun fromBrokerageQuote(brokerageQuote: BrokerageQuote, fiatCurrency: FiatCurrency, orderFee: Money?) =
            BuyQuote(
                id = brokerageQuote.id,
                // we should pass the fiat to the state, otherwise Money interface wont get serialised.
                price = brokerageQuote.sourceToDestinationRate.price.toFiat(fiatCurrency),
                availability = brokerageQuote.availability,
                settlementReason = brokerageQuote.settlementReason,
                quoteMargin = brokerageQuote.quoteMargin,
                feeDetails = BuyFees(
                    fee = fee(brokerageQuote.feeDetails.fee as FiatValue, orderFee as FiatValue),
                    feeBeforePromo = brokerageQuote.feeDetails.feeBeforePromo as FiatValue,
                    promo = brokerageQuote.feeDetails.promo
                ),
                createdAt = brokerageQuote.createdAt,
                expiresAt = brokerageQuote.expiresAt,
                remainingTime = brokerageQuote.secondsToExpire.toLong(),
                chunksTimeCounter = getListOfTotalTimes(brokerageQuote.secondsToExpire.toDouble()),
                depositTerms = brokerageQuote.depositTerms
            )

        fun fromBrokerageQuote(brokerageQuote: BrokerageQuote, fiatCurrency: FiatCurrency) =
            BuyQuote(
                id = brokerageQuote.id,
                // we should pass the fiat to the state, otherwise Money interface wont get serialised.
                price = brokerageQuote.sourceToDestinationRate.price.toFiat(fiatCurrency),
                availability = brokerageQuote.availability,
                settlementReason = brokerageQuote.settlementReason,
                quoteMargin = brokerageQuote.quoteMargin,
                feeDetails = BuyFees(
                    fee = brokerageQuote.feeDetails.fee as FiatValue,
                    feeBeforePromo = brokerageQuote.feeDetails.feeBeforePromo as FiatValue,
                    promo = brokerageQuote.feeDetails.promo
                ),
                createdAt = brokerageQuote.createdAt,
                expiresAt = brokerageQuote.expiresAt,
                remainingTime = brokerageQuote.secondsToExpire.toLong(),
                chunksTimeCounter = getListOfTotalTimes(brokerageQuote.secondsToExpire.toDouble()),
                depositTerms = brokerageQuote.depositTerms
            )

        private fun getListOfTotalTimes(remainingTime: Double): MutableList<Int> {
            val chunks = MutableList(
                floor(remainingTime / MIN_QUOTE_REFRESH).toInt()
            ) { MIN_QUOTE_REFRESH.toInt() }

            val remainder = remainingTime % MIN_QUOTE_REFRESH
            if (remainder > 0) {
                chunks.add(remainder.toInt())
            }
            return chunks
        }

        private fun fee(quoteFee: FiatValue, orderFee: FiatValue?): FiatValue =
            (
                orderFee?.let {
                    Money.max(quoteFee, it)
                } ?: quoteFee
                ) as FiatValue

        fun Money.toFiat(fiatCurrency: Currency): FiatValue {
            return (this as? CryptoValue)?.let { value ->
                return ExchangeRate(
                    from = fiatCurrency,
                    to = value.currency,
                    rate = value.toBigDecimal()
                ).inverse().price as FiatValue
            } ?: this as FiatValue
        }
    }
}

@kotlinx.serialization.Serializable
data class BuyFees(
    val feeBeforePromo: FiatValue,
    val fee: FiatValue,
    val promo: Promo
)

@kotlinx.serialization.Serializable
data class SelectedPaymentMethod(
    val id: String,
    val partner: Partner? = null,
    val label: String? = "",
    val paymentMethodType: PaymentMethodType,
    val isEligible: Boolean
) {
    fun isCard() = paymentMethodType == PaymentMethodType.PAYMENT_CARD
    fun isBank() = paymentMethodType == PaymentMethodType.BANK_TRANSFER
    fun isFunds() = paymentMethodType == PaymentMethodType.FUNDS

    fun concreteId(): String? =
        if (isDefinedBank() || isDefinedCard()) id else null

    private fun isDefinedCard() = paymentMethodType == PaymentMethodType.PAYMENT_CARD &&
        id != PaymentMethod.UNDEFINED_CARD_PAYMENT_ID

    private fun isDefinedBank() = paymentMethodType == PaymentMethodType.BANK_TRANSFER &&
        id != PaymentMethod.UNDEFINED_BANK_TRANSFER_PAYMENT_ID

    fun isActive() =
        concreteId() != null ||
            (paymentMethodType == PaymentMethodType.FUNDS && id == PaymentMethod.FUNDS_PAYMENT_ID)
}

@kotlinx.serialization.Serializable
data class GooglePayDetails(
    val tokenizationInfo: Map<String, String>? = null,
    val beneficiaryId: String? = null,
    val merchantBankCountryCode: String? = null,
    val allowPrepaidCards: Boolean = true,
    val allowCreditCards: Boolean = false,
    val allowedAuthMethods: List<String>?,
    val allowedCardNetworks: List<String>?,
    val billingAddressRequired: Boolean? = false,
    val billingAddressParameters: BillingAddressParameters? = null
)
