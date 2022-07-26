package piuk.blockchain.android.cards

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.koin.scopedInject
import com.blockchain.payments.stripe.StripeFactory
import com.blockchain.preferences.CurrencyPrefs
import com.checkout.android_sdk.PaymentForm
import com.checkout.android_sdk.Utils.Environment
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentCardVerificationBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.customviews.TransactionProgressView
import timber.log.Timber

class CardVerificationFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentCardVerificationBinding>(),
    AddCardFlowFragment {

    override val model: CardModel by scopedInject()

    private val stripeFactory: StripeFactory by inject()

    private val environmentConfig: EnvironmentConfig by inject()
    private val currencyPrefs: CurrencyPrefs by inject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCardVerificationBinding =
        FragmentCardVerificationBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.updateToolbarTitle(getString(R.string.card_verification))
        binding.checkoutCardForm.initCheckoutPaymentForm()
    }

    override fun render(newState: CardState) {
        if (newState.addCard) {
            cardDetailsPersistence.getCardData()?.let {
                model.process(CardIntent.CardAddRequested)
                model.process(CardIntent.AddNewCard(it))
            }
        }

        newState.cardRequestStatus?.let {
            when (it) {
                is CardRequestStatus.Loading -> renderLoadingState()
                is CardRequestStatus.Error -> renderErrorState(it.type)
                is CardRequestStatus.Success -> navigator.exitWithSuccess(it.card)
            }
        }

        newState.authoriseCard?.let { cardAcquirerCredentials ->
            processCardAuthRequest(cardAcquirerCredentials)
            model.process(CardIntent.ResetCardAuth)
        }
    }

    private fun processCardAuthRequest(cardAcquirerCredentials: CardAcquirerCredentials) {
        when (cardAcquirerCredentials) {
            is CardAcquirerCredentials.Everypay -> {
                openWebView(
                    cardAcquirerCredentials.paymentLink,
                    cardAcquirerCredentials.exitLink
                )
            }
            is CardAcquirerCredentials.Stripe -> {
                // TODO: Clean this up along with the one in SimpleBuyPaymentFragment
                PaymentConfiguration.init(requireContext(), cardAcquirerCredentials.apiKey)
                PaymentAuthConfig.init(
                    // Here we can customise the UI (web view) shown for 3DS
                    // via PaymentAuthConfig.Stripe3ds2UiCustomization
                    PaymentAuthConfig.Builder()
                        .set3ds2Config(
                            PaymentAuthConfig.Stripe3ds2Config.Builder()
                                .setTimeout(STRIPE_3DS_TIMEOUT_MINUTES)
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
                // Reset here in order to call CheckCardStatus safely
                model.process(CardIntent.ResetCardAuth)
                // Start polling in absence of a callback in the case of Stripe
                model.process(CardIntent.CheckCardStatus)
            }
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
                    model.process(CardIntent.CheckCardStatus)
                }
            }
        }
    }

    private fun openWebView(paymentLink: String, exitLink: String) {
        CardAuthoriseWebViewActivity.start(fragment = this, link = paymentLink, exitLink = exitLink)
    }

    private fun renderLoadingState() {
        with(binding.transactionProgressView) {
            showTxInProgress(
                title = getString(R.string.linking_card_title),
                subtitle = getString(R.string.linking_card_subtitle)
            )
        }
    }

    private fun renderErrorState(error: CardError) {
        with(binding.transactionProgressView) {
            when (error) {
                is CardError.ServerSideCardError -> {
                    showServerSideError(
                        iconUrl = error.iconUrl,
                        statusIconUrl = error.statusIconUrl,
                        title = error.title,
                        description = error.message
                    )

                    showServerSideActionErrorCtas(
                        list = error.actions,
                        currencyCode = currencyPrefs.selectedFiatCurrency.networkTicker,
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
                CardError.ActivationFailed -> showTxError(
                    title = getString(R.string.title_cardInsufficientFunds),
                    subtitle = getString(R.string.could_not_activate_card)
                )
                CardError.CardAcquirerDeclined -> showTxError(
                    title = getString(R.string.title_cardCreateBankDeclined),
                    subtitle = getString(R.string.msg_cardAcquirerDecline)
                )
                CardError.CardBankDeclined -> showTxError(
                    title = getString(R.string.title_cardBankDecline),
                    subtitle = getString(R.string.msg_cardBankDecline)
                )
                CardError.CardBlockchainDeclined -> showTxError(
                    title = getString(R.string.title_cardBlockchainDecline),
                    subtitle = getString(R.string.msg_cardBlockchainDecline)
                )
                CardError.CardCreateBankDeclined -> showTxError(
                    title = getString(R.string.title_cardCreateBankDeclined),
                    subtitle = getString(R.string.msg_cardCreateBankDeclined)
                )
                CardError.CardCreateDebitOnly -> showTxError(
                    title = getString(R.string.title_cardCreateDebitOnly),
                    subtitle = getString(R.string.msg_cardCreateDebitOnly)
                )
                CardError.CardCreateNoToken -> showTxError(
                    title = getString(R.string.title_cardCreateNoToken),
                    subtitle = getString(R.string.msg_cardCreateNoToken)
                )
                CardError.CardCreatedAbandoned -> showTxError(
                    title = getString(R.string.title_cardCreateAbandoned),
                    subtitle = getString(R.string.msg_cardCreateAbandoned)
                )
                CardError.CardCreatedExpired -> showTxError(
                    title = getString(R.string.title_cardCreateExpired),
                    subtitle = getString(R.string.msg_cardCreateExpired)
                )
                CardError.CardCreatedFailed -> showTxError(
                    title = getString(R.string.title_cardCreateFailed),
                    subtitle = getString(R.string.msg_cardCreateFailed)
                )
                CardError.CardDuplicated -> showTxError(
                    title = getString(R.string.title_cardDuplicate),
                    subtitle = getString(R.string.msg_cardDuplicate)
                )
                CardError.CardLimitReach -> showTxError(
                    title = getString(R.string.card_limit_reached_title),
                    subtitle = getString(R.string.card_limit_reached_desc)
                )
                CardError.CardPaymentDebitOnly -> showTxError(
                    title = getString(R.string.title_cardPaymentDebitOnly),
                    subtitle = getString(R.string.msg_cardPaymentDebitOnly)
                )
                CardError.CardPaymentFailed -> showTxError(
                    title = getString(R.string.title_cardPaymentFailed),
                    subtitle = getString(R.string.msg_cardPaymentFailed)
                )
                CardError.CardPaymentNotSupportedDeclined -> showTxError(
                    title = getString(R.string.title_cardPaymentNotSupported),
                    subtitle = getString(R.string.msg_cardPaymentNotSupported)
                )
                CardError.CreationFailed -> showTxError(
                    title = getString(R.string.linking_card_error_title),
                    subtitle = getString(R.string.could_not_save_card)
                )
                CardError.InsufficientCardBalance -> showTxError(
                    title = getString(R.string.title_cardInsufficientFunds),
                    subtitle = getString(R.string.msg_cardInsufficientFunds)
                )
                CardError.LinkedFailed -> showTxError(
                    title = getString(R.string.linking_card_error_title),
                    subtitle = getString(R.string.card_link_failed)
                )
                CardError.PendingAfterPoll -> showTxError(
                    title = getString(R.string.card_still_pending),
                    subtitle = getString(R.string.card_link_failed)
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EVERYPAY_AUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                model.process(CardIntent.CheckCardStatus)
                analytics.logEvent(SimpleBuyAnalytics.CARD_3DS_COMPLETED)
            }
        }
    }

    override val navigator: AddCardNavigator
        get() = (activity as? AddCardNavigator)
            ?: throw IllegalStateException("Parent must implement AddCardNavigator")

    override val cardDetailsPersistence: CardDetailsPersistence
        get() = (activity as? CardDetailsPersistence)
            ?: throw IllegalStateException("Parent must implement CardDetailsPersistence")

    override fun onBackPressed(): Boolean = true

    override fun backPressedHandled(): Boolean = true

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
                    model.process(CardIntent.CheckCardStatus)
                }

                override fun onError(errorMessage: String?) {
                    Timber.e("PaymentForm.On3DSFinished onError: $errorMessage")
                    binding.checkoutCardForm.gone()
                    model.process(CardIntent.UpdateRequestState(CardRequestStatus.Error(CardError.ActivationFailed)))
                }
            }
        )
    }

    companion object {
        const val EVERYPAY_AUTH_REQUEST_CODE = 324
        const val STRIPE_3DS_TIMEOUT_MINUTES = 5
    }
}
