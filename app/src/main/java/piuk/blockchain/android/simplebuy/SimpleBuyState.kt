package piuk.blockchain.android.simplebuy

import com.blockchain.api.NabuApiException
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ExchangePriceWithDelta
import com.blockchain.coincore.fiat.isOpenBankingCurrency
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.custodial.models.Availability
import com.blockchain.core.custodial.models.BrokerageQuote
import com.blockchain.core.custodial.models.Promo
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.payments.model.LinkBankTransfer
import com.blockchain.core.payments.model.LinkedBank
import com.blockchain.core.payments.model.Partner
import com.blockchain.core.price.ExchangeRate
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.io.Serializable
import java.math.BigInteger
import kotlinx.serialization.Contextual
import piuk.blockchain.android.cards.CardAcquirerCredentials
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionFlowStateInfo
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

/**
 * This is an object that gets serialized with Gson so any properties that we don't
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
    val kycStartedButNotCompleted: Boolean = false,
    val kycVerificationState: KycState? = null,
    val currentScreen: FlowScreen = FlowScreen.ENTER_AMOUNT,
    val selectedPaymentMethod: SelectedPaymentMethod? = null,
    val quote: BuyQuote? = null,
    val orderValue: CryptoValue? = null,
    val supportedFiatCurrencies: List<FiatCurrency> = emptyList(),
    val paymentSucceeded: Boolean = false,
    val showRating: Boolean = false,
    val withdrawalLockPeriod: @Contextual BigInteger = BigInteger.ZERO,
    val recurringBuyFrequency: RecurringBuyFrequency = RecurringBuyFrequency.ONE_TIME,
    val recurringBuyState: RecurringBuyState = RecurringBuyState.UNINITIALISED,
    val showRecurringBuyFirstTimeFlow: Boolean = false,
    val eligibleAndNextPaymentRecurringBuy: List<EligibleAndNextPaymentRecurringBuy> = emptyList(),
    val googlePayTokenizationInfo: Map<String, String>? = null,
    val googlePayBeneficiaryId: String? = null,
    val googlePayMerchantBankCountryCode: String? = null,
    val googlePayAllowPrepaidCards: Boolean? = true,
    val googlePayAllowCreditCards: Boolean? = true,
    @Transient @kotlinx.serialization.Transient val safeConnectTosLink: String? = null,
    @Transient @kotlinx.serialization.Transient val paymentOptions: PaymentOptions = PaymentOptions(),
    @Transient @kotlinx.serialization.Transient
    override val errorState: TransactionErrorState = TransactionErrorState.NONE,
    @Transient @kotlinx.serialization.Transient val buyErrorState: ErrorState? = null,
    @Transient @kotlinx.serialization.Transient override val fiatRate: ExchangeRate? = null,
    @Transient @kotlinx.serialization.Transient val exchangePriceWithDelta: ExchangePriceWithDelta? = null,
    @Transient @kotlinx.serialization.Transient val isLoading: Boolean = false,
    @Transient @kotlinx.serialization.Transient val cardAcquirerCredentials: CardAcquirerCredentials? = null,
    @Transient @kotlinx.serialization.Transient val authorisePaymentUrl: String? = null,
    @Transient @kotlinx.serialization.Transient val linkedBank: LinkedBank? = null,
    @Transient @kotlinx.serialization.Transient val shouldShowUnlockHigherFunds: Boolean = false,
    @Transient @kotlinx.serialization.Transient val linkBankTransfer: LinkBankTransfer? = null,
    @Transient @kotlinx.serialization.Transient val paymentPending: Boolean = false,
    @Transient @kotlinx.serialization.Transient val paymentFailed: Boolean = false,
    @Transient @kotlinx.serialization.Transient private val transferLimits: TxLimits? = null,
    @Transient @kotlinx.serialization.Transient override val transactionsLimit: TransactionsLimit? = null,
    // we use this flag to avoid navigating back and forth, reset after navigating
    @Transient @kotlinx.serialization.Transient val confirmationActionRequested: Boolean = false,
    @Transient @kotlinx.serialization.Transient val newPaymentMethodToBeAdded: PaymentMethod? = null
) : MviState, TransactionFlowStateInfo {

    @delegate:Transient
    val order: SimpleBuyOrder by unsafeLazy {
        SimpleBuyOrder(
            orderState,
            amount
        )
    }

    @delegate:Transient
    private val recurringBuyEligiblePaymentMethods: List<PaymentMethodType> by lazy {
        eligibleAndNextPaymentRecurringBuy.flatMap { it.eligibleMethods }
            .distinct()
    }

    @delegate:Transient
    val selectedPaymentMethodDetails: PaymentMethod? by unsafeLazy {
        selectedPaymentMethod?.id?.let { id ->
            paymentOptions.availablePaymentMethods.firstOrNull { it.id == id }
        }
    }

    @delegate:Transient
    val selectedPaymentMethodLimits: TxLimits by unsafeLazy {
        selectedPaymentMethodDetails?.let {
            TxLimits.fromAmounts(min = it.limits.min, max = it.limits.max)
        } ?: TxLimits.withMinAndUnlimitedMax(FiatValue.zero(fiatCurrency))
    }

    @delegate:Transient
    val coinHasZeroMargin: Boolean by unsafeLazy {
        quote?.quoteMargin == 0.toDouble()
    }

    @delegate:Transient
    val buyOrderLimits: TxLimits by unsafeLazy {
        transferLimits ?: TxLimits.withMinAndUnlimitedMax(FiatValue.zero(fiatCurrency))
    }

    @delegate:Transient
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

    override val action: AssetAction
        get() = AssetAction.Buy

    override val sourceAccountType: AssetCategory
        get() = if (selectedPaymentMethod?.paymentMethodType == PaymentMethodType.FUNDS) {
            AssetCategory.CUSTODIAL
        } else {
            AssetCategory.NON_CUSTODIAL
        }

    override val sendingAsset: AssetInfo?
        get() = null

    override val receivingAsset: Currency
        get() = selectedCryptoAsset ?: throw IllegalStateException("Receiving asset is empty")

    override val availableBalance: Money?
        get() = selectedPaymentMethodDetails?.availableBalance
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

enum class FlowScreen {
    ENTER_AMOUNT, KYC, KYC_VERIFICATION
}

sealed class ErrorState : Serializable {
    object BankLinkingTimeout : ErrorState()
    object LinkedBankNotSupported : ErrorState()
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
}

data class SimpleBuyOrder(
    val orderState: OrderState = OrderState.UNINITIALISED,
    val amount: FiatValue? = null
)

data class PaymentOptions(
    val availablePaymentMethods: List<PaymentMethod> = emptyList()
) {
    val canAddCard: Boolean
        get() = availablePaymentMethods.filterIsInstance<PaymentMethod.UndefinedCard>()
            .firstOrNull()?.isEligible ?: false
    val canLinkFunds: Boolean
        get() = availablePaymentMethods.filterIsInstance<PaymentMethod.UndefinedBankAccount>().firstOrNull()?.isEligible
            ?: false
    val canLinkBank: Boolean
        get() = availablePaymentMethods.filterIsInstance<PaymentMethod.UndefinedBankAccount>().firstOrNull()?.isEligible
            ?: false
}

@kotlinx.serialization.Serializable
data class BuyQuote(
    val id: String? = null,
    val price: FiatValue,
    val availability: Availability? = null,
    val quoteMargin: Double? = null,
    val feeDetails: BuyFees
) {
    companion object {
        fun fromBrokerageQuote(brokerageQuote: BrokerageQuote, fiatCurrency: FiatCurrency, orderFee: Money?) =
            BuyQuote(
                id = brokerageQuote.id,
                // we should pass the fiat to the state, otherwise Money interface wont get serialised.
                price = brokerageQuote.price.toFiat(fiatCurrency),
                availability = brokerageQuote.availability,
                quoteMargin = brokerageQuote.quoteMargin,
                feeDetails = BuyFees(
                    fee = fee(brokerageQuote.feeDetails.fee as FiatValue, orderFee as FiatValue),
                    feeBeforePromo = brokerageQuote.feeDetails.feeBeforePromo as FiatValue,
                    promo = brokerageQuote.feeDetails.promo
                )
            )

        private fun fee(quoteFee: FiatValue, orderFee: FiatValue?): FiatValue =
            (
                orderFee?.let {
                    Money.max(quoteFee, it)
                } ?: quoteFee
                ) as FiatValue

        private fun Money.toFiat(fiatCurrency: Currency): FiatValue {
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
