package piuk.blockchain.android.ui.createwallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.base.setContent
import com.blockchain.commonarch.presentation.mvi_v2.MVIActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.koin.payloadScope
import com.google.android.gms.recaptcha.RecaptchaActionType
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.cards.CountryPickerItem
import piuk.blockchain.android.cards.PickerItem
import piuk.blockchain.android.cards.PickerItemListener
import piuk.blockchain.android.cards.SearchPickerItemBottomSheet
import piuk.blockchain.android.cards.StatePickerItem
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.login.GoogleReCaptchaClient
import piuk.blockchain.android.ui.settings.security.pin.PinActivity
import timber.log.Timber

class CreateWalletActivity :
    MVIActivity<CreateWalletViewState>(),
    NavigationRouter<CreateWalletNavigation>,
    PickerItemListener,
    HostedBottomSheet.Host,
    AndroidScopeComponent {

    private lateinit var backPressCallback: OnBackPressedCallback

    override val scope: Scope = payloadScope
    override val alwaysDisableScreenshots: Boolean = true

    private val viewModel: CreateWalletViewModel by viewModel()

    private val environmentConfig: EnvironmentConfig by inject()
    private val fraudService: FraudService by inject()

    private val recaptchaClient: GoogleReCaptchaClient by lazy {
        GoogleReCaptchaClient(this, environmentConfig)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fraudService.trackFlow(FraudFlow.SIGNUP)

        recaptchaClient.initReCaptcha()
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)

        backPressCallback = onBackPressedDispatcher.addCallback {
            viewModel.onIntent(CreateWalletIntent.BackClicked)
        }

        setContent {
            CreateWalletScreen(
                viewState = viewModel.viewState,
                onIntent = viewModel::onIntent,
                showCountryBottomSheet = {
                    showBottomSheet(
                        SearchPickerItemBottomSheet.newInstance(
                            it.countries.map { country ->
                                CountryPickerItem(country.countryCode)
                            },
                            it.suggested?.let { CountryPickerItem(it.countryCode) }
                        )
                    )
                },
                showStateBottomSheet = {
                    showBottomSheet(
                        SearchPickerItemBottomSheet.newInstance(
                            it.states.map { state ->
                                StatePickerItem(state.stateCode, state.name)
                            }
                        )
                    )
                }
            )
        }
    }

    override fun onStateUpdated(state: CreateWalletViewState) {
        // handled by compose
    }

    override fun onDestroy() {
        recaptchaClient.close()
        super.onDestroy()
    }

    override fun onItemPicked(item: PickerItem) {
        when (item) {
            is CountryPickerItem -> {
                viewModel.onIntent(CreateWalletIntent.CountryInputChanged(item.code))
            }
            is StatePickerItem -> {
                viewModel.onIntent(CreateWalletIntent.StateInputChanged(item.code))
            }
        }
        hideKeyboard()
    }

    override fun route(navigationEvent: CreateWalletNavigation) {
        when (navigationEvent) {
            CreateWalletNavigation.Back -> {
                backPressCallback.remove()
                onBackPressedDispatcher.onBackPressed()
                fraudService.endFlow(FraudFlow.SIGNUP)
            }
            is CreateWalletNavigation.PinEntry -> {
                hideKeyboard()
                startActivity(
                    PinActivity.newIntent(
                        context = this,
                        startForResult = false,
                        originScreen = PinActivity.Companion.OriginScreenToPin.CREATE_WALLET,
                        addFlagsToClear = true,
                        referralCode = navigationEvent.referralCode
                    )
                )
            }
            is CreateWalletNavigation.RecaptchaVerification -> verifyReCaptcha()
        }
    }

    override fun onSheetClosed() {
        // no-op
    }

    private fun verifyReCaptcha() {
        recaptchaClient.verify(
            verificationType = RecaptchaActionType.SIGNUP,
            onSuccess = { result ->
                viewModel.onIntent(CreateWalletIntent.RecaptchaVerificationSucceeded(result.tokenResult))
            },
            onError = {
                Timber.e(it)
                viewModel.onIntent(CreateWalletIntent.RecaptchaVerificationFailed(it))
            }
        )
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, CreateWalletActivity::class.java))
        }
    }
}
