package piuk.blockchain.android.cards

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.koin.scopedInject
import com.blockchain.payments.stripe.StripeFactory
import com.checkout.android_sdk.PaymentForm
import com.checkout.android_sdk.Utils.Environment
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentCardVerificationBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import timber.log.Timber

class CardVerificationFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentCardVerificationBinding>(),
    AddCardFlowFragment {

    override val model: CardModel by scopedInject()

    private val stripeFactory: StripeFactory by inject()

    private val environmentConfig: EnvironmentConfig by inject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCardVerificationBinding =
        FragmentCardVerificationBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.updateToolbarTitle(getString(R.string.card_verification))

        with(binding.primaryBtn) {
            buttonState = ButtonState.Enabled
            text = getString(R.string.common_ok)
            onClick = {
                navigator.exitWithError()
            }
        }
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
            binding.progress.visibility = View.GONE
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
        with(binding) {
            progress.visibility = View.VISIBLE
            icon.visibility = View.GONE
            primaryBtn.visibility = View.GONE
            secondaryBtn.visibility = View.GONE
            title.text = getString(R.string.linking_card_title)
            subtitle.text = getString(R.string.linking_card_subtitle)
        }
    }

    private fun renderErrorState(error: CardError) {
        with(binding) {
            progress.visibility = View.GONE
            icon.visibility = View.VISIBLE
            primaryBtn.visibility = View.VISIBLE

            if (error == CardError.CARD_CREATE_DEBIT_ONLY) {
                renderCreditCardNotSupportedButtonsError()
            } else {
                renderButtonsError()
            }

            title.text = getString(
                when (error) {
                    CardError.INSUFFICIENT_CARD_BALANCE -> R.string.title_cardInsufficientFunds
                    CardError.CARD_BANK_DECLINED -> R.string.title_cardBankDecline
                    CardError.CARD_DUPLICATE -> R.string.title_cardDuplicate
                    CardError.CARD_BLOCKCHAIN_DECLINED -> R.string.title_cardBlockchainDecline
                    CardError.CARD_ACQUIRER_DECLINED -> R.string.title_cardAcquirerDecline
                    CardError.CARD_PAYMENT_NOT_SUPPORTED -> R.string.title_cardPaymentNotSupported
                    CardError.CARD_CREATED_FAILED -> R.string.title_cardCreateFailed
                    CardError.CARD_PAYMENT_FAILED -> R.string.title_cardPaymentFailed
                    CardError.CARD_CREATED_ABANDONED -> R.string.title_cardCreateAbandoned
                    CardError.CARD_CREATED_EXPIRED -> R.string.title_cardCreateExpired
                    CardError.CARD_CREATE_BANK_DECLINED -> R.string.title_cardCreateBankDeclined
                    CardError.CARD_CREATE_DEBIT_ONLY -> R.string.title_cardCreateDebitOnly
                    CardError.CARD_PAYMENT_DEBIT_ONLY -> R.string.title_cardPaymentDebitOnly
                    CardError.CARD_CREATE_NO_TOKEN -> R.string.title_cardCreateNoToken
                    else -> R.string.linking_card_error_title
                }
            )

            subtitle.text = getString(
                when (error) {
                    CardError.INSUFFICIENT_CARD_BALANCE -> R.string.msg_cardInsufficientFunds
                    CardError.CARD_BANK_DECLINED -> R.string.msg_cardBankDecline
                    CardError.CARD_DUPLICATE -> R.string.msg_cardDuplicate
                    CardError.CARD_BLOCKCHAIN_DECLINED -> R.string.msg_cardBlockchainDecline
                    CardError.CARD_ACQUIRER_DECLINED -> R.string.msg_cardAcquirerDecline
                    CardError.CARD_PAYMENT_NOT_SUPPORTED -> R.string.msg_cardPaymentNotSupported
                    CardError.CARD_CREATED_FAILED -> R.string.msg_cardCreateFailed
                    CardError.CARD_PAYMENT_FAILED -> R.string.msg_cardPaymentFailed
                    CardError.CARD_CREATED_ABANDONED -> R.string.msg_cardCreateAbandoned
                    CardError.CARD_CREATED_EXPIRED -> R.string.msg_cardCreateExpired
                    CardError.CARD_CREATE_BANK_DECLINED -> R.string.msg_cardCreateBankDeclined
                    CardError.CARD_CREATE_DEBIT_ONLY -> R.string.msg_cardCreateDebitOnly
                    CardError.CARD_PAYMENT_DEBIT_ONLY -> R.string.msg_cardPaymentDebitOnly
                    CardError.CARD_CREATE_NO_TOKEN -> R.string.msg_cardCreateNoToken
                    CardError.CREATION_FAILED -> R.string.could_not_save_card
                    CardError.ACTIVATION_FAIL -> R.string.could_not_activate_card
                    CardError.PENDING_AFTER_POLL -> R.string.card_still_pending
                    CardError.LINK_FAILED -> R.string.card_link_failed
                }
            )
        }
    }

    private fun renderCreditCardNotSupportedButtonsError() {
        with(binding) {
            with(primaryBtn) {
                buttonState = ButtonState.Enabled
                text = getString(R.string.card_activation_debit_only_cta_primary)
                onClick = { navigator.navigateToCardDetails() }
            }
            with(secondaryBtn) {
                visibility = View.VISIBLE
                buttonState = ButtonState.Enabled
                text = getString(R.string.common_cancel)
                onClick = { navigator.exitWithError() }
            }
        }
    }

    private fun renderButtonsError() {
        with(binding) {
            with(primaryBtn) {
                buttonState = ButtonState.Enabled
                text = getString(R.string.common_ok)
                onClick = { navigator.exitWithError() }
            }
            secondaryBtn.visibility = View.GONE
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
                    model.process(CardIntent.UpdateRequestState(CardRequestStatus.Error(CardError.ACTIVATION_FAIL)))
                }
            }
        )
    }

    companion object {
        const val EVERYPAY_AUTH_REQUEST_CODE = 324
        const val STRIPE_3DS_TIMEOUT_MINUTES = 5
    }
}
