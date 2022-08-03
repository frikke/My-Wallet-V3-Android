package piuk.blockchain.android.simplebuy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.api.NabuApiException
import com.blockchain.banking.BankPaymentApproval
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.payments.stripe.StripeFactory
import com.blockchain.utils.secondsToDays
import com.checkout.android_sdk.PaymentForm
import com.checkout.android_sdk.Utils.Environment
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatValue
import java.util.Locale
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.cards.CardAcquirerCredentials
import piuk.blockchain.android.cards.CardAuthoriseWebViewActivity
import piuk.blockchain.android.cards.CardVerificationFragment
import piuk.blockchain.android.databinding.FragmentSimpleBuyPaymentBinding
import piuk.blockchain.android.rating.presentaion.AppRatingFragment
import piuk.blockchain.android.rating.presentaion.AppRatingTriggerSource
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INSUFFICIENT_FUNDS
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INTERNET_CONNECTION_ERROR
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.OVER_MAXIMUM_SOURCE_LIMIT
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.PENDING_ORDERS_LIMIT_REACHED
import piuk.blockchain.android.simplebuy.sheets.UnlockHigherLimitsBottomSheet
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.customviews.TransactionProgressView
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.recurringbuy.subtitleForLockedFunds
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiserImpl.Companion.getEstimatedTransactionCompletionTime
import piuk.blockchain.android.util.StringUtils
import timber.log.Timber

class SimpleBuyPaymentFragment :
    MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState, FragmentSimpleBuyPaymentBinding>(),
    SimpleBuyScreen,
    UnlockHigherLimitsBottomSheet.Host {

    override val model: SimpleBuyModel by scopedInject()
    private val stripeFactory: StripeFactory by inject()
    private val deeplinkRedirector: DeeplinkRedirector by scopedInject()
    private val environmentConfig: EnvironmentConfig by inject()
    private var isFirstLoad = false
    private lateinit var previousSelectedPaymentMethodId: String
    private lateinit var previousSelectedCryptoAsset: AssetInfo

    private val isPaymentAuthorised: Boolean by lazy {
        arguments?.getBoolean(IS_PAYMENT_AUTHORISED, false) ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFirstLoad = savedInstanceState == null
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSimpleBuyPaymentBinding =
        FragmentSimpleBuyPaymentBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.updateToolbarTitle(getString(R.string.common_payment))

        binding.checkoutCardForm.initCheckoutPaymentForm()
    }

    override fun render(newState: SimpleBuyState) {
        require(newState.selectedPaymentMethod != null)
        require(newState.selectedCryptoAsset != null)

        previousSelectedPaymentMethodId = newState.selectedPaymentMethod.id
        previousSelectedCryptoAsset = newState.selectedCryptoAsset

        newState.selectedCryptoAsset.let {
            binding.transactionProgressView.setAssetIcon(it)
        }

        if (newState.paymentSucceeded) {
            model.process(SimpleBuyIntent.CheckForOrderCompletedSideEvents)
        }

        if (newState.buyErrorState != null) {
            newState.orderValue?.currencyCode?.let { currencyCode ->
                handleErrorStates(newState.buyErrorState, currencyCode)
            }
            return
        }

        if (newState.orderState == OrderState.CANCELED) {
            navigator().exitSimpleBuyFlow()
            return
        }

        if (newState.recurringBuyState == RecurringBuyState.INACTIVE) {
            BlockchainSnackbar.make(
                binding.root, getString(R.string.recurring_buy_creation_error), Snackbar.LENGTH_SHORT,
                SnackbarType.Error
            ).show()
        }

        if (newState.orderState == OrderState.AWAITING_FUNDS && isFirstLoad) {
            if (isPaymentAuthorised) {
                model.process(SimpleBuyIntent.CheckOrderStatus)
            } else {
                model.process(SimpleBuyIntent.MakePayment(newState.id ?: return))
            }
            isFirstLoad = false
        }

        binding.transactionProgressView.setupPrimaryCta(getString(R.string.common_ok)) {
            when {
                newState.showRecurringBuyFirstTimeFlow -> {
                    navigator().goToSetupFirstRecurringBuy()
                }
                !newState.paymentPending -> navigator().exitSimpleBuyFlow()
                else -> navigator().goToPendingOrderScreen()
            }
        }

        renderTitleAndSubtitle(newState)

        newState.cardAcquirerCredentials?.let { cardAcquirerCredentials ->
            processCardAuthRequest(cardAcquirerCredentials)
            model.process(SimpleBuyIntent.ResetCardPaymentAuth)
        }

        if (newState.shouldLaunchExternalFlow()) {
            newState.order.amount?.let { orderValue ->
                launchExternalAuthoriseUrlFlow(
                    // !! here is safe because the state check validates nullability
                    newState.id!!, newState.authorisePaymentUrl!!, newState.linkedBank!!, orderValue
                )
            }
        }

        if (newState.showAppRating) {
            showAppRating()
        }
    }

    private fun processCardAuthRequest(cardAcquirerCredentials: CardAcquirerCredentials) {
        when (cardAcquirerCredentials) {
            is CardAcquirerCredentials.Checkout -> {
                // For Checkout no 3DS is required if the link is empty. For Stripe they take care of all scenarios.
                if (cardAcquirerCredentials.paymentLink.isNotEmpty()) {
                    binding.checkoutCardForm.apply {
                        visible()
                        setKey(cardAcquirerCredentials.apiKey)
                        handle3DS(
                            cardAcquirerCredentials.paymentLink,
                            cardAcquirerCredentials.exitLink,
                            cardAcquirerCredentials.exitLink
                        )
                    }
                } else {
                    model.process(SimpleBuyIntent.CheckOrderStatus)
                }
            }
            is CardAcquirerCredentials.Everypay -> openWebView(
                cardAcquirerCredentials.paymentLink,
                cardAcquirerCredentials.exitLink
            )
            is CardAcquirerCredentials.Stripe -> {
                PaymentConfiguration.init(requireContext(), cardAcquirerCredentials.apiKey)
                PaymentAuthConfig.init(
                    // Here we can customise the UI (web view) shown for 3DS
                    // via PaymentAuthConfig.Stripe3ds2UiCustomization
                    PaymentAuthConfig.Builder()
                        .set3ds2Config(
                            PaymentAuthConfig.Stripe3ds2Config.Builder()
                                .setTimeout(CardVerificationFragment.STRIPE_3DS_TIMEOUT_MINUTES)
                                .build()
                        )
                        .build()
                )
                stripeFactory.getOrCreate(cardAcquirerCredentials.apiKey)
                    .confirmPayment(
                        fragment = this,
                        confirmPaymentIntentParams = ConfirmPaymentIntentParams.create(
                            clientSecret = cardAcquirerCredentials.clientSecret
                        )
                    )
                // Reset here in order to call CheckOrderStatus safely
                model.process(SimpleBuyIntent.ResetCardPaymentAuth)
                // Start polling in absence of a callback in the case of Stripe
                model.process(SimpleBuyIntent.CheckOrderStatus)
            }
        }
    }

    private fun addLink(stringResource: Int): CharSequence {
        return StringUtils.getStringWithMappedAnnotations(
            requireContext(),
            stringResource,
            emptyMap()
        ) {
            startActivity(SupportCentreActivity.newIntent(requireContext(), SUPPORT_SB_SUBJECT))
            requireActivity().finish()
        }
    }

    private fun logErrorAnalytics(
        title: String,
        error: String,
        nabuApiException: NabuApiException? = null,
        errorDescription: String
    ) {
        analytics.logEvent(
            ClientErrorAnalytics.ClientLogError(
                nabuApiException = nabuApiException,
                error = error,
                errorDescription = errorDescription,
                source = nabuApiException?.getErrorCode()?.let { ClientErrorAnalytics.Companion.Source.NABU }
                    ?: ClientErrorAnalytics.Companion.Source.CLIENT,
                title = title,
                action = ClientErrorAnalytics.ACTION_BUY,
                categories = nabuApiException?.getServerSideErrorInfo()?.categories ?: emptyList()
            )
        )
    }

    private fun handleErrorStates(errorState: ErrorState, currencyCode: String) {
        when (errorState) {
            ErrorState.ApproveBankInvalid,
            ErrorState.ApprovedBankAccountInvalid,
            -> showError(
                title = getString(R.string.bank_transfer_payment_invalid_title),
                subtitle = addLink(R.string.bank_transfer_payment_invalid_subtitle),
                resourceIcon = R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.ApprovedBankFailed,
            ErrorState.ApprovedBankFailedInternal,
            -> showError(
                title = getString(R.string.bank_transfer_payment_failed_title),
                subtitle = addLink(R.string.bank_transfer_payment_failed_subtitle),
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.ApprovedBankDeclined -> showError(
                title = getString(R.string.bank_transfer_payment_declined_title),
                subtitle = addLink(R.string.bank_transfer_payment_declined_subtitle),
                resourceIcon = R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.ApprovedBankRejected -> showError(
                title = getString(R.string.bank_transfer_payment_rejected_title),
                subtitle = addLink(R.string.bank_transfer_payment_rejected_subtitle),
                resourceIcon = R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.ApprovedBankExpired -> showError(
                title = getString(R.string.bank_transfer_payment_expired_title),
                subtitle = addLink(R.string.bank_transfer_payment_expired_subtitle),
                resourceIcon = R.drawable.ic_pending_icon_circle,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.ApprovedBankLimitedExceed -> showError(
                title = getString(R.string.bank_transfer_payment_limited_exceeded_title),
                subtitle = addLink(R.string.bank_transfer_payment_limited_exceeded_subtitle),
                resourceIcon = R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.ApprovedBankInsufficientFunds -> showError(
                title = getString(R.string.bank_transfer_payment_insufficient_funds_title),
                subtitle = addLink(R.string.bank_transfer_payment_insufficient_funds_subtitle),
                resourceIcon = R.drawable.ic_cross_white_bckg,
                errorState = INSUFFICIENT_FUNDS,
                currencyCode = currencyCode
            )
            is ErrorState.PaymentFailedError -> showError(
                title = getString(R.string.payment_failed_title_with_reason),
                subtitle = addLink(R.string.sb_checkout_contact_support),
                resourceIcon = R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            is ErrorState.ApprovedBankUndefinedError -> showError(
                title = getString(R.string.payment_failed_title_with_reason),
                subtitle = addLink(R.string.sb_checkout_contact_support),
                resourceIcon = R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.InternetConnectionError -> showError(
                title = getString(
                    R.string.executing_connection_error
                ),
                subtitle = addLink(R.string.sb_checkout_contact_support),
                resourceIcon = R.drawable.ic_cross_white_bckg,
                errorState = INTERNET_CONNECTION_ERROR,
                currencyCode = currencyCode
            )
            is ErrorState.UnhandledHttpError -> showError(
                title = getString(
                    R.string.common_http_error_with_new_line_message, errorState.nabuApiException.getErrorDescription()
                ),
                subtitle = addLink(R.string.sb_checkout_contact_support),
                resourceIcon = R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                nabuApiException = errorState.nabuApiException,
                currencyCode = currencyCode
            )
            ErrorState.DailyLimitExceeded -> showError(
                getString(R.string.sb_checkout_daily_limit_title),
                getString(R.string.sb_checkout_daily_limit_blurb),
                R.drawable.ic_cross_white_bckg,
                errorState = OVER_MAXIMUM_SOURCE_LIMIT,
                currencyCode = currencyCode
            )
            ErrorState.WeeklyLimitExceeded -> showError(
                getString(R.string.sb_checkout_weekly_limit_title),
                getString(R.string.sb_checkout_weekly_limit_blurb),
                R.drawable.ic_cross_white_bckg,
                errorState = OVER_MAXIMUM_SOURCE_LIMIT,
                currencyCode = currencyCode
            )
            ErrorState.YearlyLimitExceeded -> showError(
                getString(R.string.sb_checkout_yearly_limit_title),
                getString(R.string.sb_checkout_yearly_limit_blurb),
                R.drawable.ic_cross_white_bckg,
                errorState = OVER_MAXIMUM_SOURCE_LIMIT,
                currencyCode = currencyCode
            )
            ErrorState.ExistingPendingOrder -> showError(
                getString(R.string.sb_checkout_pending_order_title),
                getString(R.string.sb_checkout_pending_order_blurb),
                R.drawable.ic_cross_white_bckg,
                errorState = PENDING_ORDERS_LIMIT_REACHED,
                currencyCode = currencyCode
            )
            ErrorState.InsufficientCardFunds -> showError(
                getString(R.string.title_cardInsufficientFunds),
                getString(R.string.msg_cardInsufficientFunds),
                R.drawable.ic_cross_white_bckg,
                errorState = INSUFFICIENT_FUNDS,
                currencyCode = currencyCode
            )
            ErrorState.CardBankDeclined -> showError(
                getString(R.string.title_cardBankDecline),
                getString(R.string.msg_cardBankDecline),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardDuplicated -> showError(
                getString(R.string.title_cardDuplicate),
                getString(R.string.msg_cardDuplicate),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardBlockchainDeclined -> showError(
                getString(R.string.title_cardBlockchainDecline),
                getString(R.string.msg_cardBlockchainDecline),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardAcquirerDeclined -> showError(
                getString(R.string.title_cardAcquirerDecline),
                getString(R.string.msg_cardAcquirerDecline),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardPaymentNotSupported -> showError(
                getString(R.string.title_cardPaymentNotSupported),
                getString(R.string.msg_cardPaymentNotSupported),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardCreateFailed -> showError(
                getString(R.string.title_cardCreateFailed),
                getString(R.string.msg_cardCreateFailed),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardPaymentFailed -> showError(
                getString(R.string.title_cardPaymentFailed),
                getString(R.string.msg_cardPaymentFailed),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardCreateAbandoned -> showError(
                getString(R.string.title_cardCreateAbandoned),
                getString(R.string.msg_cardCreateAbandoned),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardCreateExpired -> showError(
                getString(R.string.title_cardCreateExpired),
                getString(R.string.msg_cardCreateExpired),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardCreateBankDeclined -> showError(
                getString(R.string.title_cardCreateBankDeclined),
                getString(R.string.msg_cardCreateBankDeclined),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardCreateDebitOnly -> showError(
                getString(R.string.title_cardCreateDebitOnly),
                getString(R.string.msg_cardCreateDebitOnly),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardPaymentDebitOnly -> showError(
                getString(R.string.title_cardPaymentDebitOnly),
                getString(R.string.msg_cardPaymentDebitOnly),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.CardNoToken -> showError(
                getString(R.string.title_cardCreateNoToken),
                getString(R.string.msg_cardCreateNoToken),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            is ErrorState.BankLinkMaxAccountsReached -> showError(
                getString(R.string.bank_linking_max_accounts_title),
                getString(R.string.bank_linking_max_accounts_subtitle),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                nabuApiException = errorState.error,
                currencyCode = currencyCode
            )
            is ErrorState.BankLinkMaxAttemptsReached -> showError(
                getString(R.string.bank_linking_max_attempts_title),
                getString(R.string.bank_linking_max_attempts_subtitle),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                nabuApiException = errorState.error,
                currencyCode = currencyCode
            )
            ErrorState.LinkedBankNotSupported -> throw IllegalStateException(
                " ErrorState LinkedBankNotSupported should not get handled in Payments screen"
            )
            ErrorState.UnknownCardProvider,
            ErrorState.ProviderIsNotSupported,
            -> showError(
                getString(R.string.sb_card_provider_not_supported),
                getString(R.string.sb_checkout_contact_support),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.BankLinkingTimeout -> showError(
                getString(R.string.bank_linking_timeout_error_title),
                getString(R.string.bank_linking_timeout_error_subtitle),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            ErrorState.Card3DsFailed -> showError(
                getString(R.string.card_3ds),
                getString(R.string.sb_checkout_contact_support),
                R.drawable.ic_cross_white_bckg,
                errorState = errorState.toString(),
                currencyCode = currencyCode
            )
            is ErrorState.ServerSideUxError -> showServerSideError(
                errorState.serverSideUxErrorInfo,
                currencyCode = currencyCode
            )
            ErrorState.BuyPaymentMethodsUnavailable,
            ErrorState.SettlementGenericError,
            ErrorState.SettlementInsufficientBalance,
            is ErrorState.SettlementRefreshRequired,
            ErrorState.SettlementStaleBalance -> {
                // no-op this is not handled here
            }
        }
    }

    private fun showServerSideError(
        serverSideUxErrorInfo: ServerSideUxErrorInfo,
        currencyCode: String,
    ) {
        logErrorAnalytics(
            title = serverSideUxErrorInfo.title,
            error = "ServerSideUxError",
            errorDescription = serverSideUxErrorInfo.description
        )
        with(binding.transactionProgressView) {
            setupErrorButtons(serverSideUxErrorInfo.actions, currencyCode)
            showServerSideError(
                serverSideUxErrorInfo.iconUrl,
                serverSideUxErrorInfo.statusUrl,
                serverSideUxErrorInfo.title,
                serverSideUxErrorInfo.description
            )
        }
    }

    private fun showError(
        title: String,
        subtitle: CharSequence,
        resourceIcon: Int = R.drawable.ic_alert_white_bkgd,
        errorState: String,
        nabuApiException: NabuApiException? = null,
        currencyCode: String,
    ) {
        logErrorAnalytics(
            title = title,
            error = errorState,
            nabuApiException = nabuApiException,
            errorDescription = subtitle.toString()
        )
        with(binding) {
            transactionProgressView.apply {
                setupErrorButtons(emptyList(), currencyCode)
                showTxError(
                    title = title,
                    subtitle = subtitle,
                    statusIcon = resourceIcon
                )
            }
        }
    }

    private fun TransactionProgressView.setupErrorButtons(list: List<ServerErrorAction>, currencyCode: String) {
        if (list.isEmpty()) {
            setupPrimaryCta(
                text = getString(R.string.common_try_again),
                onClick = {
                    navigator().popFragmentsInStackUntilFind(
                        fragmentName = SimpleBuyCheckoutFragment::class.simpleName.orEmpty(),
                        popInclusive = true
                    )
                }
            )
            setupSecondaryCta(
                text = getString(R.string.bank_transfer_transfer_go_back),
                onClick = {
                    navigator().exitSimpleBuyFlow()
                }
            )
        } else {
            showServerSideActionErrorCtas(
                list = list,
                currencyCode = currencyCode,
                onActionsClickedCallback = object : TransactionProgressView.TransactionProgressActions {
                    override fun onPrimaryButtonClicked() {
                        activity.finish()
                    }

                    override fun onSecondaryButtonClicked() {
                        activity.finish()
                    }

                    override fun onTertiaryButtonClicked() {
                        activity.finish()
                    }
                }
            )
        }
    }

    override fun onPause() {
        binding.transactionProgressView.clearSubscriptions()
        super.onPause()
    }

    private fun launchExternalAuthoriseUrlFlow(
        paymentId: String,
        authorisationUrl: String,
        linkedBank: LinkedBank,
        orderValue: FiatValue,
    ) {
        startActivityForResult(
            BankAuthActivity.newInstance(
                BankPaymentApproval(
                    paymentId = paymentId,
                    authorisationUrl = authorisationUrl,
                    linkedBank = linkedBank,
                    orderValue = orderValue
                ),
                BankAuthSource.SIMPLE_BUY, requireContext()
            ),
            BANK_APPROVAL
        )
    }

    private fun showAppRating() {
        AppRatingFragment.newInstance(AppRatingTriggerSource.BUY)
            .show(childFragmentManager, AppRatingFragment.TAG)

        model.process(SimpleBuyIntent.AppRatingShown)
    }

    private fun renderTitleAndSubtitle(newState: SimpleBuyState) {
        require(newState.selectedPaymentMethod != null)
        when {
            newState.paymentSucceeded && newState.orderValue != null -> {
                val lockedFundDays = newState.withdrawalLockPeriod.secondsToDays()
                val messageOnPayment = if (newState.recurringBuyState == RecurringBuyState.ACTIVE) {
                    getString(
                        R.string.recurring_buy_payment_message,
                        newState.order.amount?.toStringWithSymbol(),
                        newState.recurringBuyFrequency.toHumanReadableRecurringBuy(requireContext())
                            .toLowerCase(Locale.getDefault()),
                        newState.orderValue.currency.name,
                        newState.selectedCryptoAsset?.displayTicker
                    )
                } else {
                    getString(
                        R.string.card_purchased_available_now,
                        newState.orderValue.currency.name
                    )
                }

                if (lockedFundDays <= 0L) {
                    binding.transactionProgressView.showTxSuccess(
                        title = getString(R.string.card_purchased, newState.orderValue.formatOrSymbolForZero()),
                        subtitle = messageOnPayment
                    )
                } else {
                    binding.transactionProgressView.showPendingTx(
                        title = getString(R.string.card_purchased, newState.orderValue.formatOrSymbolForZero()),
                        subtitle = messageOnPayment,
                        locksNote = newState.selectedPaymentMethod.paymentMethodType
                            .subtitleForLockedFunds(
                                lockedFundDays,
                                requireContext()
                            )
                    )
                }
                checkForUnlockHigherLimits(newState.shouldShowUnlockHigherFunds)
            }
            newState.isLoading && newState.orderValue != null -> {
                binding.transactionProgressView.showTxInProgress(
                    getString(R.string.card_buying, newState.orderValue.formatOrSymbolForZero()),
                    getString(
                        R.string.completing_card_buy_1,
                        newState.order.amount?.toStringWithSymbol(),
                        newState.selectedCryptoAsset?.displayTicker
                    ) + appendRecurringBuyInfo(
                        order = newState.order,
                        selectedCryptoAsset = newState.selectedCryptoAsset,
                        recurringBuyFrequency = newState.recurringBuyFrequency
                    )
                )
            }
            newState.paymentPending && newState.orderValue != null -> {
                when (newState.selectedPaymentMethod.paymentMethodType) {
                    PaymentMethodType.BANK_TRANSFER -> {
                        binding.transactionProgressView.showTxPending(
                            getString(
                                R.string.bank_transfer_in_progress_title, newState.orderValue.formatOrSymbolForZero()
                            ),
                            newState.linkBankTransfer?.partner?.let {
                                when (it) {
                                    BankPartner.YAPILY -> {
                                        getString(R.string.bank_transfer_in_progress_ob_blurb)
                                    }
                                    BankPartner.YODLEE, BankPartner.PLAID -> {
                                        getString(
                                            R.string.bank_transfer_in_progress_blurb,
                                            getEstimatedTransactionCompletionTime()
                                        )
                                    }
                                }
                            } ?: (
                                getString(
                                    R.string.completing_card_buy_1,
                                    newState.order.amount?.toStringWithSymbol(),
                                    newState.selectedCryptoAsset?.displayTicker
                                ) + appendRecurringBuyInfo(
                                    order = newState.order,
                                    selectedCryptoAsset = newState.selectedCryptoAsset,
                                    recurringBuyFrequency = newState.recurringBuyFrequency
                                )
                                )
                        )
                    }
                    else -> {
                        binding.transactionProgressView.showTxPending(
                            getString(R.string.card_in_progress, newState.orderValue.formatOrSymbolForZero()),
                            getString(R.string.we_will_notify_order_complete)
                        )
                    }
                }
            }
        }
    }

    private fun appendRecurringBuyInfo(
        order: SimpleBuyOrder,
        selectedCryptoAsset: AssetInfo?,
        recurringBuyFrequency: RecurringBuyFrequency,
    ): String {
        return if (recurringBuyFrequency != RecurringBuyFrequency.ONE_TIME) {
            "\n" + getString(
                R.string.completing_card_buy_rb,
                order.amount?.toStringWithSymbol(),
                selectedCryptoAsset?.displayTicker,
                recurringBuyFrequency.toHumanReadableRecurringBuy(requireContext())
            )
        } else ""
    }

    private fun checkForUnlockHigherLimits(shouldShowUnlockMoreFunds: Boolean) {
        if (!shouldShowUnlockMoreFunds)
            return
        binding.transactionProgressView.setupSecondaryCta(
            text = getString(R.string.want_to_buy_more)
        ) {
            showBottomSheet(UnlockHigherLimitsBottomSheet())
        }
    }

    private fun openWebView(paymentLink: String, exitLink: String) {
        CardAuthoriseWebViewActivity.start(fragment = this, link = paymentLink, exitLink = exitLink)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CardVerificationFragment.EVERYPAY_AUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                model.process(SimpleBuyIntent.CheckOrderStatus)
                analytics.logEvent(SimpleBuyAnalytics.CARD_3DS_COMPLETED)
            } else {
                cancelAndGoBackToEnterAmountScreen()
            }
        }
        if (requestCode == SimpleBuyActivity.KYC_STARTED &&
            resultCode == SimpleBuyActivity.RESULT_KYC_SIMPLE_BUY_COMPLETE
        ) {
            navigator().exitSimpleBuyFlow()
        }

        if (requestCode == BANK_APPROVAL && resultCode == Activity.RESULT_CANCELED) {
            cancelAndGoBackToEnterAmountScreen()
        }
    }

    private fun cancelAndGoBackToEnterAmountScreen() {
        model.process(SimpleBuyIntent.CancelOrderAndResetAuthorisation)
        navigator().popFragmentsInStackUntilFind(
            fragmentName = SimpleBuyCheckoutFragment::class.simpleName.orEmpty(),
            popInclusive = true
        )
    }

    override fun unlockHigherLimits() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, SimpleBuyActivity.KYC_STARTED)
        analytics.logEvent(SDDAnalytics.UPGRADE_TO_GOLD_CLICKED)
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException(
            "Parent must implement SimpleBuyNavigator"
        )

    override fun onBackPressed(): Boolean = true

    override fun backPressedHandled(): Boolean {
        return true
    }

    private fun PaymentForm.initCheckoutPaymentForm() {
        if (environmentConfig.environment == com.blockchain.enviroment.Environment.PRODUCTION) {
            setEnvironment(Environment.LIVE)
        } else {
            setEnvironment(Environment.SANDBOX)
        }
        set3DSListener(
            object : PaymentForm.On3DSFinished {
                override fun onSuccess(token: String?) {
                    binding.checkoutCardForm.gone()
                    model.process(SimpleBuyIntent.CheckOrderStatus)
                }

                override fun onError(errorMessage: String?) {
                    Timber.e("PaymentForm.On3DSFinished onError: $errorMessage")
                    binding.checkoutCardForm.gone()
                    model.process(SimpleBuyIntent.ErrorIntent(ErrorState.Card3DsFailed))
                }
            }
        )
    }

    companion object {
        private const val IS_PAYMENT_AUTHORISED = "IS_PAYMENT_AUTHORISED"
        private const val BANK_APPROVAL = 5123
        private const val SUPPORT_SB_SUBJECT = "Issue with Payments"

        fun newInstance(isFromDeepLink: Boolean) =
            SimpleBuyPaymentFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_PAYMENT_AUTHORISED, isFromDeepLink)
                }
            }
    }
}
