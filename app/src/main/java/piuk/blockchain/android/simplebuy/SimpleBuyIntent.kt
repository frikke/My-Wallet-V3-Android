package piuk.blockchain.android.simplebuy

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.core.custodial.models.BrokerageQuote
import com.blockchain.core.limits.TxLimits
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.domain.paymentmethods.model.GooglePayAddress
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.domain.trade.model.RecurringBuyFrequency
import com.blockchain.domain.trade.model.RecurringBuyState
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.payments.googlepay.manager.request.BillingAddressParameters
import com.blockchain.presentation.complexcomponents.QuickFillButtonData
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.math.BigInteger
import piuk.blockchain.android.cards.CardAcquirerCredentials
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState

sealed class SimpleBuyIntent : MviIntent<SimpleBuyState> {

    override fun isValidFor(oldState: SimpleBuyState): Boolean {
        return oldState.buyErrorState == null
    }

    override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState

    object InitializeFeatureFlags : SimpleBuyIntent()

    class UpdateFeatureFlags(
        private val featureFlagSet: FeatureFlagsSet
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(featureFlagSet = featureFlagSet)
    }

    object ShowAppRating : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(showAppRating = true)

        override fun isValidFor(oldState: SimpleBuyState) = oldState.showAppRating.not()
    }

    class GetQuotePrice(
        val currencyPair: CurrencyPair,
        val amount: Money,
        val paymentMethod: PaymentMethodType
    ) : SimpleBuyIntent()

    class UpdateQuotePrice(
        private val amountInCrypto: CryptoValue,
        private val dynamicFee: Money,
        private val fiatPrice: Money
    ) :
        SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                quotePrice = QuotePrice(
                    amountInCrypto = amountInCrypto,
                    fee = dynamicFee as FiatValue,
                    fiatPrice = fiatPrice as FiatValue
                ),
            )
    }

    object StopPollingQuotePrice : SimpleBuyIntent()

    object AppRatingShown : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(showAppRating = false)

        override fun isValidFor(oldState: SimpleBuyState) = oldState.showAppRating
    }

    class AmountUpdated(val amount: FiatValue) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(amount = amount)

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return oldState.amount != amount
        }
    }

    class PreselectedAmountUpdated(val amount: FiatValue) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(amount = amount, hasAmountComeFromDeeplink = true)

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return oldState.amount != amount
        }
    }

    class GetPrefillAndQuickFillAmounts(
        val limits: TxLimits,
        val assetCode: String,
        val fiatCurrency: FiatCurrency,
        val usePrefilledAmount: Boolean,
    ) : SimpleBuyIntent()

    class PrefillEnterAmount(
        val amount: Money,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            require(amount is FiatValue)
            return oldState.copy(amount = amount)
        }

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return amount.isPositive
        }
    }

    class PopulateQuickFillButtons(
        private val quickFillButtonData: QuickFillButtonData
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(quickFillButtonData = quickFillButtonData)
    }

    object ResetLinkBankTransfer : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(linkBankTransfer = null, newPaymentMethodToBeAdded = null)
    }

    class UpdateErrorState(private val errorState: TransactionErrorState) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(errorState = errorState)
    }

    class Open3dsAuth(private val cardAcquirerCredentials: CardAcquirerCredentials) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                cardAcquirerCredentials = cardAcquirerCredentials,
                hasHandled3ds = true
            )
    }

    data class OpenCvvInput(val paymentId: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                securityCodePaymentId = paymentId,
                openCvvInput = true,
            )
    }

    object OpenCvvInputHandled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(openCvvInput = false, hasHandledCvv = true)
    }

    class CvvInputResult(val cvvUpdateSuccessful: Boolean) : SimpleBuyIntent()

    class AuthorisePaymentExternalUrl(private val url: String, private val linkedBank: LinkedBank) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(authorisePaymentUrl = url, linkedBank = linkedBank)
    }

    class PaymentMethodChangeRequested(val paymentMethod: PaymentMethod) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(
            errorState = TransactionErrorState.NONE
        )
    }

    class FetchPaymentDetails(
        val fiatCurrency: FiatCurrency,
        val selectedPaymentMethodId: String,
    ) : SimpleBuyIntent()

    class PaymentMethodsUpdated(
        val paymentOptions: PaymentOptions,
        val selectedPaymentMethod: SelectedPaymentMethod?,
        val usePrefilledAmount: Boolean,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(
                isLoading = false
            )
        }
    }

    object GooglePayInfoRequested : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(
            isLoading = true
        )

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return true
        }
    }

    class GooglePayInfoReceived(
        private val tokenizationData: Map<String, String>,
        private val beneficiaryId: String,
        private val merchantBankCountryCode: String,
        private val allowPrepaidCards: Boolean,
        private val allowCreditCards: Boolean,
        private val allowedAuthMethods: List<String>,
        private val allowedCardNetworks: List<String>,
        private val billingAddressRequired: Boolean,
        private val billingAddressParameters: BillingAddressParameters,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(
                isLoading = false,
                googlePayDetails = GooglePayDetails(
                    tokenizationInfo = tokenizationData,
                    beneficiaryId = beneficiaryId,
                    merchantBankCountryCode = merchantBankCountryCode,
                    allowPrepaidCards = allowPrepaidCards,
                    allowCreditCards = allowCreditCards,
                    allowedAuthMethods = allowedAuthMethods,
                    allowedCardNetworks = allowedCardNetworks,
                    billingAddressRequired = billingAddressRequired,
                    billingAddressParameters = billingAddressParameters
                )
            )
        }
    }

    object ClearGooglePayTokenizationInfo : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                googlePayDetails = oldState.googlePayDetails?.copy(
                    tokenizationInfo = null
                )
            )

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return oldState.googlePayDetails?.tokenizationInfo != null
        }
    }

    class SelectedPaymentMethodUpdate(
        val paymentMethod: PaymentMethod,
    ) : SimpleBuyIntent() {
        // UndefinedBankAccount has no visual representation in the UI, it just opens WireTransferDetails,
        // hence why we're ignoring it and keeping the current selectedPaymentMethod
        override fun isValidFor(oldState: SimpleBuyState): Boolean =
            paymentMethod !is PaymentMethod.UndefinedBankAccount

        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                selectedPaymentMethod = SelectedPaymentMethod(
                    id = paymentMethod.id,
                    // no partner for bank transfer or ui label. Ui label for bank transfer is coming from resources
                    partner = (paymentMethod as? PaymentMethod.Card)?.partner,
                    label = paymentMethod.detailedLabel(),
                    paymentMethodType = when (paymentMethod) {
                        is PaymentMethod.UndefinedBankTransfer -> PaymentMethodType.BANK_TRANSFER
                        is PaymentMethod.UndefinedCard -> PaymentMethodType.PAYMENT_CARD
                        is PaymentMethod.Bank -> PaymentMethodType.BANK_TRANSFER
                        is PaymentMethod.Funds -> PaymentMethodType.FUNDS
                        is PaymentMethod.UndefinedBankAccount -> PaymentMethodType.FUNDS
                        is PaymentMethod.GooglePay -> PaymentMethodType.GOOGLE_PAY
                        else -> PaymentMethodType.PAYMENT_CARD
                    },
                    isEligible = paymentMethod.isEligible
                )
            )
    }

    object EnterAmountBuyButtonClicked : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = true, orderState = OrderState.INITIALISED)
    }

    object LinkBankTransferRequested : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(
            newPaymentMethodToBeAdded = null
        )
    }

    object TryToLinkABankTransfer : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(isLoading = true)
        }
    }

    data class UpdatedBuyLimitsAndPaymentMethods(
        private val limits: TxLimits,
        private val paymentOptions: PaymentOptions,
        private val selectedPaymentMethod: SelectedPaymentMethod?,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(
                transferLimits = limits,
                paymentOptions = paymentOptions,
                selectedPaymentMethod = selectedPaymentMethod
            )
        }
    }

    data class WithdrawLocksTimeUpdated(private val time: BigInteger = BigInteger.ZERO) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(withdrawalLockPeriod = time)
        }
    }

    data class InitialiseSelectedCryptoAndFiat(val asset: AssetInfo, val fiatCurrency: FiatCurrency) :
        SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(selectedCryptoAsset = asset, fiatCurrency = fiatCurrency)
    }

    data class FlowCurrentScreen(val flowScreen: FlowScreen) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(currentScreen = flowScreen)
    }

    data class FetchSuggestedPaymentMethod(
        val fiatCurrency: FiatCurrency,
        val selectedPaymentMethodId: String? = null,
        val usePrefilledAmount: Boolean = true,
        val reloadQuickFillButtons: Boolean = true
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentOptions = PaymentOptions(), selectedPaymentMethod = null)
    }

    object FetchEligibility : SimpleBuyIntent()

    data class UpgradeEligibilityTransactionsLimit(
        val transactionsLimit: TransactionsLimit,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(transactionsLimit = transactionsLimit)
    }

    object CancelOrder : SimpleBuyIntent() {
        override fun isValidFor(oldState: SimpleBuyState) = true
    }

    object CancelOrderAndResetAuthorisation : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                authorisePaymentUrl = null,
                linkedBank = null
            )
    }

    object ClearState : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            SimpleBuyState()

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return oldState.orderState < OrderState.PENDING_CONFIRMATION ||
                oldState.orderState > OrderState.PENDING_EXECUTION
        }
    }

    class ConfirmOrder(val orderId: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                confirmationActionRequested = true,
                isLoading = true,
                id = orderId
            )
    }

    // Feynman Specific
    class CreateAndConfirmOrder(
        val googlePayPayload: String? = null,
        val googlePayAddress: GooglePayAddress? = null,
        val recurringBuyFrequency: RecurringBuyFrequency? = RecurringBuyFrequency.ONE_TIME
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                isLoading = true,
                recurringBuyFrequency = recurringBuyFrequency ?: oldState.recurringBuyFrequency
            )
    }

    data class ConfirmGooglePayOrder(
        val orderId: String?,
        val googlePayPayload: String,
        val googlePayAddress: GooglePayAddress?
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                confirmationActionRequested = true,
                isLoading = true,
                id = orderId
            )
    }

    object FetchWithdrawLockTime : SimpleBuyIntent()

    object NavigationHandled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = false, newPaymentMethodToBeAdded = null)
    }

    object KycStarted : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                kycStartedButNotCompleted = true,
                currentScreen = FlowScreen.KYC,
                kycVerificationState = null
            )
    }

    class ErrorIntent(private val error: ErrorState) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                buyErrorState = error,
                isLoading = false,
                confirmationActionRequested = false
            )

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return true
        }
    }

    object KycCompleted : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycStartedButNotCompleted = false)
    }

    object FetchKycState : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycVerificationState = KycState.PENDING)
    }

    class KycStateUpdated(val kycState: KycState) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycVerificationState = kycState)
    }

    class BankLinkProcessStarted(private val bankTransfer: LinkBankTransfer) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(
                linkBankTransfer = bankTransfer,
                confirmationActionRequested = false,
                newPaymentMethodToBeAdded = null,
                isLoading = false
            )
        }
    }

    object OrderCanceled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            SimpleBuyState(
                orderState = OrderState.CANCELED
            )
    }

    class OrderCreated(
        private val buyOrder: BuySellOrder,
        private val quote: BrokerageQuote,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                orderState = buyOrder.state,
                failureReason = buyOrder.failureReason,
                id = buyOrder.id,
                quote = BuyQuote.fromBrokerageQuote(
                    brokerageQuote = quote,
                    fiatCurrency = buyOrder.source.currency as FiatCurrency,
                    orderFee = buyOrder.fee,
                ),
                // TODO(aromano): QUOTE with feynmanFF off we use the order crypto value instead of the quote
                orderValue = buyOrder.orderValue as CryptoValue,
                paymentSucceeded = buyOrder.state == OrderState.FINISHED,
                isLoading = false,
                recurringBuyState = if (buyOrder.recurringBuyId.isNullOrBlank()) {
                    RecurringBuyState.UNINITIALISED
                } else {
                    RecurringBuyState.ACTIVE
                },
                hasQuoteChanged = oldState.quote?.id != null && (oldState.quote.id != quote.id)
            )
    }

    class UpdateBrokerageQuote(
        private val quote: BrokerageQuote,
        private val currencySource: Currency,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                quote = BuyQuote.fromBrokerageQuote(
                    brokerageQuote = quote,
                    fiatCurrency = currencySource as FiatCurrency
                ),
                hasQuoteChanged = oldState.quote?.id != null && (oldState.quote.id != quote.id)
            )
    }

    class OrderConfirmed(
        private val buyOrder: BuySellOrder,
        private val isRbActive: Boolean
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                orderState = buyOrder.state,
                failureReason = buyOrder.failureReason,
                paymentSucceeded = buyOrder.state == OrderState.FINISHED,
                isLoading = false,
                orderValue = buyOrder.orderValue as CryptoValue,
                recurringBuyState = if (isRbActive || buyOrder.recurringBuyId?.isNotEmpty() == true) {
                    RecurringBuyState.ACTIVE
                } else {
                    RecurringBuyState.UNINITIALISED
                }
            )
    }

    class UpdateSelectedPaymentCard(
        private val id: String,
        private val label: String?,
        private val partner: Partner,
        private val isEligible: Boolean,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                selectedPaymentMethod = SelectedPaymentMethod(
                    id, partner, label, PaymentMethodType.PAYMENT_CARD, isEligible
                )
            )
    }

    object ClearError : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(buyErrorState = null, errorState = TransactionErrorState.NONE)

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return oldState.buyErrorState != null
        }
    }

    object ResetCardPaymentAuth : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(cardAcquirerCredentials = null)
    }

    object ListenToOrderCreation : SimpleBuyIntent()

    object ListenToQuotesUpdate : SimpleBuyIntent()

    object GetBrokerageQuote : SimpleBuyIntent()

    class StartPollingBrokerageQuotes(val brokerageQuote: BrokerageQuote) : SimpleBuyIntent()

    object StopPollingBrokerageQuotes : SimpleBuyIntent()

    class StopQuotesUpdate(val shouldResetOrder: Boolean) : SimpleBuyIntent()

    class SelectedPaymentChangedLimits(
        val selectedPaymentMethod: SelectedPaymentMethod?,
        val limits: TxLimits
    ) : SimpleBuyIntent()

    object CancelOrderIfAnyAndCreatePendingOne : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                isLoading = true
            )

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return oldState.selectedCryptoAsset != null &&
                oldState.order.amount != null &&
                oldState.orderState != OrderState.AWAITING_FUNDS &&
                oldState.orderState != OrderState.PENDING_EXECUTION
        }
    }

    class MakePayment(val orderId: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(isLoading = true)
    }

    class GetAuthorisationUrl(val orderId: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(isLoading = true)
    }

    object CheckOrderStatus : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(isLoading = true)
    }

    object PaymentSucceeded : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentSucceeded = true, isLoading = false)
    }

    object UnlockHigherLimits : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(shouldShowUnlockHigherFunds = true)
    }

    object PollingTimedOutWithPaymentPending : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentPending = true, isLoading = false)
    }

    class AddNewPaymentMethodRequested(private val paymentMethod: PaymentMethod) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(newPaymentMethodToBeAdded = paymentMethod)
    }

    class UpdatePaymentMethodsAndAddTheFirstEligible(val fiatCurrency: FiatCurrency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                paymentOptions = PaymentOptions(),
                errorState = TransactionErrorState.NONE,
                selectedPaymentMethod = null
            )
    }

    object AddNewPaymentMethodHandled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(newPaymentMethodToBeAdded = null)
    }

    object GetRecurringBuyFrequencyRemote : SimpleBuyIntent()

    class UpdateRecurringFrequencyRemote(private val recurringBuyFrequencyRemote: RecurringBuyFrequency) :
        SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(suggestedRecurringBuyExperiment = recurringBuyFrequencyRemote)
    }

    class RecurringBuyIntervalUpdated(private val recurringBuyFrequency: RecurringBuyFrequency) :
        SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(recurringBuyFrequency = recurringBuyFrequency)
    }

    class CreateRecurringBuy(val recurringBuyFrequency: RecurringBuyFrequency) : SimpleBuyIntent()

    class RecurringBuyCreated(
        val recurringBuyId: String,
        val recurringBuyFrequency: RecurringBuyFrequency,
    ) :
        SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                recurringBuyState = RecurringBuyState.ACTIVE,
                recurringBuyFrequency = recurringBuyFrequency,
                recurringBuyId = recurringBuyId
            )
    }

    class RecurringBuyEligibilityUpdated(
        private val eligibilityNextPaymentList: List<EligibleAndNextPaymentRecurringBuy>,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                eligibleAndNextPaymentRecurringBuy = eligibilityNextPaymentList
            )
    }

    class ToggleRecurringBuy(private val isRecurringBuyToggled: Boolean) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                isRecurringBuyToggled = isRecurringBuyToggled
            )

        override fun isValidFor(oldState: SimpleBuyState): Boolean =
            oldState.isRecurringBuyToggled != isRecurringBuyToggled
    }

    class TxLimitsUpdated(private val limits: TxLimits) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                transferLimits = limits
            )
    }

    object GetSafeConnectTermsOfServiceLink : SimpleBuyIntent()

    data class UpdateSafeConnectTermsOfServiceLink(
        private val termsOfServiceLink: String,
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                safeConnectTosLink = termsOfServiceLink
            )
    }

    object OrderFinishedSuccessfully : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(orderFinishedSuccessfullyHandled = true)

        override fun isValidFor(oldState: SimpleBuyState) = !oldState.orderFinishedSuccessfullyHandled
    }
}
