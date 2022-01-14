package piuk.blockchain.android.simplebuy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.banking.BankPaymentApproval
import com.blockchain.core.payments.model.BankPartner
import com.blockchain.core.payments.model.LinkedBank
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.payments.stripe.StripeFactory
import com.blockchain.preferences.RatingPrefs
import com.blockchain.utils.secondsToDays
import com.checkout.android_sdk.PaymentForm
import com.checkout.android_sdk.Utils.Environment
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
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
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.recurringbuy.subtitleForLockedFunds
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiserImpl.Companion.getEstimatedTransactionCompletionTime
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

class SimpleBuyPaymentFragment :
    MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState, FragmentSimpleBuyPaymentBinding>(),
    SimpleBuyScreen,
    UnlockHigherLimitsBottomSheet.Host {

    override val model: SimpleBuyModel by scopedInject()
    private val stripeFactory: StripeFactory by inject()
    private val environmentConfig: EnvironmentConfig by inject()
    private val ratingPrefs: RatingPrefs by scopedInject()
    private var reviewInfo: ReviewInfo? = null
    private var isFirstLoad = false

    private val isPaymentAuthorised: Boolean by lazy {
        arguments?.getBoolean(IS_PAYMENT_AUTHORISED, false) ?: false
    }

    private val reviewManager by lazy {
        ReviewManagerFactory.create(activity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFirstLoad = savedInstanceState == null
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSimpleBuyPaymentBinding =
        FragmentSimpleBuyPaymentBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.updateToolbar(getString(R.string.common_payment))

        // we need to make the request as soon as possible and cache the result
        if (!ratingPrefs.hasSeenRatingDialog) {
            reviewManager.requestReviewFlow()
                .addOnCompleteListener { request ->
                    if (request.isSuccessful) {
                        reviewInfo = request.result
                    }
                }
        }
        binding.checkoutCardForm.initCheckoutPaymentForm()
    }

    override fun render(newState: SimpleBuyState) {
        newState.selectedCryptoAsset?.let {
            binding.transactionProgressView.setAssetIcon(it)
        }

        newState.buyErrorState?.let {
            handleErrorStates(it)
        }

        if (newState.orderState == OrderState.CANCELED) {
            navigator().exitSimpleBuyFlow()
            return
        }

        if (newState.recurringBuyState == RecurringBuyState.INACTIVE) {
            toast(resources.getString(R.string.recurring_buy_creation_error), ToastCustom.TYPE_ERROR)
        }

        if (newState.orderState == OrderState.AWAITING_FUNDS && isFirstLoad) {
            if (isPaymentAuthorised) {
                model.process(SimpleBuyIntent.CheckOrderStatus)
            } else {
                model.process(SimpleBuyIntent.MakePayment(newState.id ?: return))
            }
            isFirstLoad = false
        }

        require(newState.selectedPaymentMethod != null)

        renderTitleAndSubtitle(newState)

        binding.transactionProgressView.onCtaClick {
            when {
                newState.showRecurringBuyFirstTimeFlow -> {
                    navigator().goToSetupFirstRecurringBuy()
                }
                !newState.paymentPending -> navigator().exitSimpleBuyFlow()
                else -> navigator().goToPendingOrderScreen()
            }
        }

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

        if (newState.showRating) {
            tryToShowInAppRating()
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

    private fun handleErrorStates(errorState: ErrorState) =
        when (errorState) {
            ErrorState.ApprovedBankDeclined -> showError(
                getString(R.string.bank_linking_declined_title), getString(R.string.bank_linking_declined_subtitle)
            )
            ErrorState.ApprovedBankRejected -> showError(
                getString(R.string.bank_linking_rejected_title), getString(R.string.bank_linking_rejected_subtitle)
            )
            ErrorState.ApprovedBankFailed -> showError(
                getString(R.string.bank_linking_failure_title), getString(R.string.bank_linking_failure_subtitle)
            )
            ErrorState.ApprovedBankExpired -> showError(
                getString(R.string.bank_linking_expired_title), getString(R.string.bank_linking_expired_subtitle)
            )
            ErrorState.ApprovedGenericError -> showError(
                getString(R.string.common_oops), getString(R.string.common_error)
            )
            else -> {
                // do nothing - we only want to handle OB approval errors in this fragment
            }
        }

    private fun showError(title: String, subtitle: String) {
        binding.transactionProgressView.onCtaClick {
            navigator().exitSimpleBuyFlow()
        }
        binding.transactionProgressView.showTxError(title, subtitle)
    }

    private fun launchExternalAuthoriseUrlFlow(
        paymentId: String,
        authorisationUrl: String,
        linkedBank: LinkedBank,
        orderValue: FiatValue
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

    private fun tryToShowInAppRating() {
        reviewInfo?.let {
            val flow = reviewManager.launchReviewFlow(activity, it)
            flow.addOnCompleteListener {
                model.process(SimpleBuyIntent.AppRatingShown)
            }
        }
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
                                    BankPartner.YODLEE -> {
                                        getString(
                                            R.string.bank_transfer_in_progress_blurb,
                                            getEstimatedTransactionCompletionTime()
                                        )
                                    }
                                }
                            } ?: getString(
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
                    else -> {
                        binding.transactionProgressView.showTxPending(
                            getString(R.string.card_in_progress, newState.orderValue.formatOrSymbolForZero()),
                            getString(R.string.we_will_notify_order_complete)
                        )
                    }
                }
            }

            newState.buyErrorState != null -> {
                binding.transactionProgressView.showTxError(
                    getString(R.string.common_oops),
                    if (newState.recurringBuyFrequency != RecurringBuyFrequency.ONE_TIME) {
                        getString(
                            R.string.order_error_subtitle_rb,
                            newState.order.amount?.formatOrSymbolForZero(),
                            newState.selectedCryptoAsset?.displayTicker
                        )
                    } else {
                        getString(R.string.order_error_subtitle)
                    }
                )
            }
        }
    }

    private fun appendRecurringBuyInfo(
        order: SimpleBuyOrder,
        selectedCryptoAsset: AssetInfo?,
        recurringBuyFrequency: RecurringBuyFrequency
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
        binding.transactionProgressView.configureSecondaryButton(getString(R.string.want_to_buy_more)) {
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
                model.process(SimpleBuyIntent.ErrorIntent())
            }
        }
        if (requestCode == SimpleBuyActivity.KYC_STARTED &&
            resultCode == SimpleBuyActivity.RESULT_KYC_SIMPLE_BUY_COMPLETE
        ) {
            navigator().exitSimpleBuyFlow()
        }

        if (requestCode == BANK_APPROVAL && resultCode == Activity.RESULT_CANCELED) {
            model.process(SimpleBuyIntent.CancelOrderAndResetAuthorisation)
        }
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
        if (environmentConfig.environment == info.blockchain.wallet.api.Environment.PRODUCTION) {
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
                    model.process(SimpleBuyIntent.ErrorIntent())
                }
            }
        )
    }

    companion object {
        private const val IS_PAYMENT_AUTHORISED = "IS_PAYMENT_AUTHORISED"
        private const val BANK_APPROVAL = 5123

        fun newInstance(isFromDeepLink: Boolean) =
            SimpleBuyPaymentFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_PAYMENT_AUTHORISED, isFromDeepLink)
                }
            }
    }
}
