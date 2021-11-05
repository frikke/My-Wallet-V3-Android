package piuk.blockchain.android.simplebuy

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ExchangePriceWithDelta
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialQuote
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.Partner
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.LinkedBank
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.io.Serializable
import java.math.BigInteger
import java.util.Date
import piuk.blockchain.android.cards.EverypayAuthOptions
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionFlowStateInfo
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

/**
 * This is an object that gets serialized with Gson so any properties that we don't
 * want to get serialized should be tagged as @Transient
 *
 */
data class SimpleBuyState constructor(
    val id: String? = null,
    val fiatCurrency: String = "USD",
    override val amount: FiatValue = FiatValue.zero(fiatCurrency),
    val selectedCryptoAsset: AssetInfo? = null,
    val orderState: OrderState = OrderState.UNINITIALISED,
    private val expirationDate: Date? = null,
    val custodialQuote: CustodialQuote? = null,
    val kycStartedButNotCompleted: Boolean = false,
    val kycVerificationState: KycState? = null,
    val currentScreen: FlowScreen = FlowScreen.ENTER_AMOUNT,
    val selectedPaymentMethod: SelectedPaymentMethod? = null,
    val orderExchangePrice: FiatValue? = null,
    val orderValue: CryptoValue? = null,
    val fee: FiatValue? = null,
    val supportedFiatCurrencies: List<String> = emptyList(),
    val paymentSucceeded: Boolean = false,
    val showRating: Boolean = false,
    val withdrawalLockPeriod: BigInteger = BigInteger.ZERO,
    val recurringBuyFrequency: RecurringBuyFrequency = RecurringBuyFrequency.ONE_TIME,
    val recurringBuyState: RecurringBuyState = RecurringBuyState.UNINITIALISED,
    val showRecurringBuyFirstTimeFlow: Boolean = false,
    val eligibleAndNextPaymentRecurringBuy: List<EligibleAndNextPaymentRecurringBuy> = emptyList(),
    @Transient val paymentOptions: PaymentOptions = PaymentOptions(),
    @Transient override val errorState: TransactionErrorState = TransactionErrorState.NONE,
    @Transient val buyErrorState: ErrorState? = null,
    @Transient override val fiatRate: ExchangeRate? = null,
    @Transient val exchangePriceWithDelta: ExchangePriceWithDelta? = null,
    @Transient val isLoading: Boolean = false,
    @Transient val everypayAuthOptions: EverypayAuthOptions? = null,
    @Transient val authorisePaymentUrl: String? = null,
    @Transient val linkedBank: LinkedBank? = null,
    @Transient val shouldShowUnlockHigherFunds: Boolean = false,
    @Transient val linkBankTransfer: LinkBankTransfer? = null,
    @Transient val paymentPending: Boolean = false,
    @Transient val transferLimits: TxLimits = TxLimits.withMinAndUnlimitedMax(FiatValue.zero(fiatCurrency)),
    // we use this flag to avoid navigating back and forth, reset after navigating
    @Transient val confirmationActionRequested: Boolean = false,
    @Transient val newPaymentMethodToBeAdded: PaymentMethod? = null
) : MviState, TransactionFlowStateInfo {

    @delegate:Transient
    val order: SimpleBuyOrder by unsafeLazy {
        SimpleBuyOrder(
            orderState,
            amount,
            expirationDate,
            custodialQuote
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

    override val limits: TxLimits
        get() = selectedPaymentMethodLimits.combineWith(transferLimits)

    fun maxCryptoAmount(exchangeRates: ExchangeRatesDataManager): Money? =
        selectedCryptoAsset?.let {
            exchangeRates.getLastFiatToCryptoRate(
                sourceFiat = fiatCurrency,
                targetCrypto = selectedCryptoAsset
            ).convert(limits.maxAmount)
        }

    fun minCryptoAmount(exchangeRates: ExchangeRatesDataManager): Money? =
        selectedCryptoAsset?.let {
            exchangeRates.getLastFiatToCryptoRate(
                sourceFiat = fiatCurrency,
                targetCrypto = selectedCryptoAsset
            ).convert(limits.minAmount)
        }

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

    override val action: AssetAction
        get() = AssetAction.Buy

    override val sendingAsset: AssetInfo?
        get() = null

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
    ENTER_AMOUNT, KYC, KYC_VERIFICATION, CHECKOUT
}

sealed class ErrorState : Serializable {
    object GenericError : ErrorState()
    object BankLinkingUpdateFailed : ErrorState()
    object BankLinkingFailed : ErrorState()
    object BankLinkingTimeout : ErrorState()
    object LinkedBankAlreadyLinked : ErrorState()
    object LinkedBankInfoNotFound : ErrorState()
    object LinkedBankAccountUnsupported : ErrorState()
    object LinkedBankNamesMismatched : ErrorState()
    object LinkedBankNotSupported : ErrorState()
    object LinkedBankRejected : ErrorState()
    object LinkedBankExpired : ErrorState()
    object LinkedBankFailure : ErrorState()
    object LinkedBankInternalFailure : ErrorState()
    object LinkedBankInvalid : ErrorState()
    object LinkedBankFraud : ErrorState()
    object ApprovedBankDeclined : ErrorState()
    object ApprovedBankRejected : ErrorState()
    object ApprovedBankFailed : ErrorState()
    object ApprovedBankExpired : ErrorState()
    object ApprovedGenericError : ErrorState()
    object DailyLimitExceeded : ErrorState()
    object WeeklyLimitExceeded : ErrorState()
    object YearlyLimitExceeded : ErrorState()
    object ExistingPendingOrder : ErrorState()
}

data class SimpleBuyOrder(
    val orderState: OrderState = OrderState.UNINITIALISED,
    val amount: FiatValue? = null,
    val expirationDate: Date? = null,
    val custodialQuote: CustodialQuote? = null
)

data class PaymentOptions(
    val availablePaymentMethods: List<PaymentMethod> = emptyList(),
    val canAddCard: Boolean = false,
    val canLinkFunds: Boolean = false,
    val canLinkBank: Boolean = false
)

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
        concreteId() != null || (paymentMethodType == PaymentMethodType.FUNDS && id == PaymentMethod.FUNDS_PAYMENT_ID)
}
