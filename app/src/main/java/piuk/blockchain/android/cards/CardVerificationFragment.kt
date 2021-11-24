package piuk.blockchain.android.cards

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.payments.stripe.StripeFactory
import com.checkout.android_sdk.PaymentForm
import com.checkout.android_sdk.Utils.Environment
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.model.ConfirmPaymentIntentParams
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentCardVerificationBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
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
        activity.setupToolbar(getString(R.string.card_verification))
        binding.okBtn.setOnClickListener {
            navigator.exitWithError()
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
            okBtn.visibility = View.GONE
            title.text = getString(R.string.linking_card_title)
            subtitle.text = getString(R.string.linking_card_subtitle)
        }
    }

    private fun renderErrorState(error: CardError) {
        with(binding) {
            progress.visibility = View.GONE
            icon.visibility = View.VISIBLE
            okBtn.visibility = View.VISIBLE
            title.text = getString(R.string.linking_card_error_title)
            subtitle.text = when (error) {
                CardError.CREATION_FAILED -> getString(R.string.could_not_save_card)
                CardError.ACTIVATION_FAIL -> getString(R.string.could_not_activate_card)
                CardError.PENDING_AFTER_POLL -> getString(R.string.card_still_pending)
                CardError.LINK_FAILED -> getString(R.string.card_link_failed)
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
        if (environmentConfig.environment == info.blockchain.wallet.api.Environment.PRODUCTION) {
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
