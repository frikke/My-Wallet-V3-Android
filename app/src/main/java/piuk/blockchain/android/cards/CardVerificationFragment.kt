package piuk.blockchain.android.cards

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.payments.stripe.StripeFactory
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.disableBackPress
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.presentation.spinner.SpinnerAnalyticsScreen
import com.blockchain.presentation.spinner.SpinnerAnalyticsTracker
import com.checkout.android_sdk.PaymentForm
import com.checkout.android_sdk.Utils.Environment
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentCardVerificationBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
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

    private val spinnerTracker: SpinnerAnalyticsTracker by inject {
        parametersOf(
            SpinnerAnalyticsScreen.AddCard,
            lifecycleScope
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCardVerificationBinding =
        FragmentCardVerificationBinding.inflate(inflater, container, false)

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // this screen ui is mostly for loading so
        // disable back press by leaving this empty
        requireActivity().disableBackPress(owner = this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.updateToolbarTitle(getString(com.blockchain.stringResources.R.string.card_verification))
        binding.checkoutCardForm.initCheckoutPaymentForm()
    }

    override fun render(newState: CardState) {
        if (!newState.vgsTokenResponse.isNullOrEmpty() && newState.isVgsEnabled && newState.cardRequestStatus == null) {
            model.process(CardIntent.CheckCardStatus(newState.vgsTokenResponse))
        } else if (newState.addCard) {
            cardDetailsPersistence.getCardData()?.let {
                model.process(CardIntent.CardAddRequested)
                model.process(CardIntent.AddNewCard(it))
            }
        }

        newState.cardRequestStatus?.let {
            when (it) {
                is CardRequestStatus.Loading -> {
                    spinnerTracker.start()
                    renderLoadingState()
                }

                is CardRequestStatus.Error -> {
                    spinnerTracker.stop()
                    renderErrorState(it.type)
                }

                is CardRequestStatus.Success -> {
                    spinnerTracker.stop()
                    renderSuccessState(it.card)
                }
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

            is CardAcquirerCredentials.FakeCardAcquirer -> {
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
                model.process(CardIntent.CheckCardStatus())
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
                    model.process(CardIntent.CheckCardStatus())
                }
            }
        }
    }

    private fun openWebView(paymentLink: String, exitLink: String) {
        CardAuthoriseWebViewActivity.start(fragment = this, link = paymentLink, exitLink = exitLink)
    }

    private fun renderLoadingState() {
        with(binding.transactionProgressView) {
            setAssetIcon(R.drawable.ic_card_icon)
            showTxInProgress(
                title = getString(com.blockchain.stringResources.R.string.linking_card_title),
                subtitle = getString(com.blockchain.stringResources.R.string.linking_card_subtitle)
            )
        }
    }

    private fun renderSuccessState(card: PaymentMethod.Card) {
        binding.transactionProgressView.apply {
            showTxSuccess(
                title = getString(com.blockchain.stringResources.R.string.linking_card_success_title),
                subtitle = getString(com.blockchain.stringResources.R.string.linking_card_success_subtitle)
            )

            setupPrimaryCta(
                text = getString(com.blockchain.stringResources.R.string.common_ok),
                onClick = {
                    navigator.exitWithSuccess(card)
                }
            )
        }
    }

    private fun renderErrorState(error: CardError) {
        with(binding.transactionProgressView) {
            when (error) {
                is CardError.ServerSideCardError -> {
                    analytics.logEvent(
                        ClientErrorAnalytics.ClientLogError(
                            errorId = error.errorId,
                            errorDescription = error.message,
                            title = error.title,
                            source = ClientErrorAnalytics.Companion.Source.NABU,
                            error = "ServerSideCardError",
                            nabuApiException = null,
                            categories = error.categories,
                            action = "ADD_CARD"
                        )
                    )

                    showServerSideError(
                        iconUrl = error.iconUrl,
                        statusIconUrl = error.statusIconUrl,
                        title = error.title,
                        description = error.message
                    )

                    showServerSideActionErrorCtas(
                        list = error.actions.ifEmpty {
                            listOf(
                                ServerErrorAction(
                                    title = getString(com.blockchain.stringResources.R.string.common_ok),
                                    deeplinkPath = getString(com.blockchain.stringResources.R.string.empty)
                                )
                            )
                        },
                        currencyCode = currencyPrefs.selectedFiatCurrency.networkTicker,
                        onActionsClickedCallback = object : TransactionProgressView.TransactionProgressActions {
                            override fun onPrimaryButtonClicked() {
                                navigator.restartFlow()
                            }

                            override fun onSecondaryButtonClicked() {
                                navigator.restartFlow()
                            }

                            override fun onTertiaryButtonClicked() {
                                navigator.restartFlow()
                            }
                        }
                    )
                }

                CardError.ActivationFailed -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardCreateBankDeclined,
                    subtitle = com.blockchain.stringResources.R.string.could_not_activate_card
                )

                CardError.CardAcquirerDeclined -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardCreateBankDeclined,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardAcquirerDecline
                )

                CardError.CardBankDeclined -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardBankDecline,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardBankDecline
                )

                CardError.CardBlockchainDeclined -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardBlockchainDecline,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardBlockchainDecline
                )

                CardError.CardCreateBankDeclined -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardCreateBankDeclined,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardCreateBankDeclined
                )

                CardError.CardCreateDebitOnly -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardCreateDebitOnly,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardCreateDebitOnly
                )

                CardError.CardCreateNoToken -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardCreateNoToken,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardCreateNoToken
                )

                CardError.CardCreatedAbandoned -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardCreateAbandoned,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardCreateAbandoned
                )

                CardError.CardCreatedExpired -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardCreateExpired,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardCreateExpired
                )

                CardError.CardCreatedFailed -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardCreateFailed,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardCreateFailed
                )

                CardError.CardDuplicated -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardDuplicate,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardDuplicate
                )

                CardError.CardLimitReach -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.card_limit_reached_title,
                    subtitle = com.blockchain.stringResources.R.string.card_limit_reached_desc
                )

                CardError.CardPaymentDebitOnly -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardPaymentDebitOnly,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardPaymentDebitOnly
                )

                CardError.CardPaymentFailed -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardPaymentFailed,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardPaymentFailed
                )

                CardError.CardPaymentNotSupportedDeclined -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardPaymentNotSupported,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardPaymentNotSupported
                )

                CardError.CreationFailed -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.linking_card_error_title,
                    subtitle = com.blockchain.stringResources.R.string.could_not_save_card
                )

                CardError.InsufficientCardBalance -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.title_cardInsufficientFunds,
                    subtitle = com.blockchain.stringResources.R.string.msg_cardInsufficientFunds
                )

                CardError.LinkFailed -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.linking_card_error_title,
                    subtitle = com.blockchain.stringResources.R.string.card_link_failed
                )

                CardError.PendingAfterPoll -> renderLegacyError(
                    title = com.blockchain.stringResources.R.string.card_still_pending,
                    subtitle = com.blockchain.stringResources.R.string.card_link_failed
                )
            }
        }
    }

    private fun renderLegacyError(@StringRes title: Int, @StringRes subtitle: Int) {
        with(binding.transactionProgressView) {
            setupPrimaryCta(
                text = getString(com.blockchain.stringResources.R.string.common_ok),
                onClick = { navigator.restartFlow() }
            )
            showTxError(
                title = getString(title),
                subtitle = getString(subtitle)
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EVERYPAY_AUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                model.process(CardIntent.CheckCardStatus())
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
                    model.process(CardIntent.CheckCardStatus())
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
