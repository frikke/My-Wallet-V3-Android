package piuk.blockchain.android.ui.createwallet

import androidx.lifecycle.viewModelScope
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.ProviderSpecificAnalytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.referral.ReferralService
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.DefaultLabels
import info.blockchain.wallet.util.PasswordUtil
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.referral.presentation.ReferralAnalyticsEvents
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.FormatChecker
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

data class CreateWalletModelState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val passwordConfirmationInput: String = "",

    val countryInputState: CountryInputState = CountryInputState.Loading,
    val stateInputState: StateInputState = StateInputState.Hidden,

    val areTermsOfServiceChecked: Boolean = false,

    val referralCodeInput: String = "",
    val isInvalidReferralErrorShowing: Boolean = false,

    val isCreateWalletLoading: Boolean = false,

    val error: CreateWalletError? = null
) : ModelState {
    fun validateIsNextEnabled(): Boolean =
        emailInput.isNotEmpty() &&
            passwordInput.isNotEmpty() &&
            passwordConfirmationInput.length == passwordInput.length &&
            countryInputState is CountryInputState.Loaded &&
            countryInputState.selected != null &&
            stateInputState.run {
                this is StateInputState.Hidden || (this is StateInputState.Loaded && this.selected != null)
            } &&
            areTermsOfServiceChecked &&
            referralCodeInput.run {
                this.isEmpty() || this.length == REFERRAL_CODE_LENGTH
            }
}

sealed class CreateWalletNavigation : NavigationEvent {
    data class PinEntry(val referralCode: String?) : CreateWalletNavigation()
}

sealed class CreateWalletError {
    object InvalidEmail : CreateWalletError()
    object InvalidPasswordTooShort : CreateWalletError()
    object InvalidPasswordTooLong : CreateWalletError()
    object PasswordsMismatch : CreateWalletError()
    object InvalidPasswordTooWeak : CreateWalletError()
    object WalletCreationFailed : CreateWalletError()

    data class Unknown(val message: String?) : CreateWalletError()
}

class CreateWalletViewModel(
    private val environmentConfig: EnvironmentConfig,
    private val defaultLabels: DefaultLabels,
    private val authPrefs: AuthPrefs,
    private val walletStatusPrefs: WalletStatusPrefs,
    private val analytics: Analytics,
    private val specificAnalytics: ProviderSpecificAnalytics,
    private val appUtil: AppUtil,
    private val formatChecker: FormatChecker,
    private val eligibilityService: EligibilityService,
    private val referralService: ReferralService,
    private val payloadDataManager: PayloadDataManager,
) : MviViewModel<
    CreateWalletIntent,
    CreateWalletViewState,
    CreateWalletModelState,
    CreateWalletNavigation,
    ModelConfigArgs.NoArgs
    >(CreateWalletModelState()) {

    private var fetchStatesJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        viewModelScope.launch {
            eligibilityService.getCountriesList(GetRegionScope.Signup)
                .doOnSuccess { countries ->
                    val localisedCountries = countries.map {
                        val locale = Locale("", it.countryCode)
                        it.copy(name = locale.displayCountry)
                    }
                    updateState { it.copy(countryInputState = CountryInputState.Loaded(localisedCountries, null)) }
                }
                .doOnFailure { error ->
                    updateState {
                        it.copy(
                            countryInputState = CountryInputState.Loaded(emptyList(), null),
                            error = CreateWalletError.Unknown(error.message)
                        )
                    }
                }
        }
    }

    override fun reduce(state: CreateWalletModelState): CreateWalletViewState = CreateWalletViewState(
        emailInput = state.emailInput,
        passwordInput = state.passwordInput,
        passwordConfirmationInput = state.passwordConfirmationInput,
        countryInputState = state.countryInputState,
        stateInputState = state.stateInputState,
        areTermsOfServiceChecked = state.areTermsOfServiceChecked,
        referralCodeInput = state.referralCodeInput,
        isInvalidReferralErrorShowing = state.isInvalidReferralErrorShowing,
        isCreateWalletLoading = state.isCreateWalletLoading,
        isNextEnabled = state.validateIsNextEnabled(),
        error = state.error
    )

    override suspend fun handleIntent(
        modelState: CreateWalletModelState,
        intent: CreateWalletIntent
    ) {
        when (intent) {
            is CreateWalletIntent.EmailInputChanged -> updateState {
                it.copy(emailInput = intent.input)
            }
            is CreateWalletIntent.PasswordInputChanged -> updateState {
                it.copy(passwordInput = intent.input)
            }
            is CreateWalletIntent.PasswordConfirmationInputChanged -> updateState {
                it.copy(passwordConfirmationInput = intent.input)
            }
            is CreateWalletIntent.CountryInputChanged -> {
                fetchStatesJob?.cancel()
                require(modelState.countryInputState is CountryInputState.Loaded)
                val countryInputState = modelState.countryInputState
                val newCountry = countryInputState.countries.find { it.countryCode == intent.countryCode }!!

                val hasStates = newCountry.states.isNotEmpty()

                updateState {
                    it.copy(
                        countryInputState = countryInputState.copy(selected = newCountry),
                        stateInputState = if (!hasStates) StateInputState.Hidden else StateInputState.Loading
                    )
                }
                if (hasStates) {
                    fetchStatesJob = viewModelScope.launch {
                        eligibilityService.getStatesList(newCountry.countryCode, GetRegionScope.Signup)
                            .doOnSuccess { states ->
                                updateState { it.copy(stateInputState = StateInputState.Loaded(states, null)) }
                            }
                            .doOnFailure { error ->
                                updateState {
                                    it.copy(
                                        stateInputState = StateInputState.Loaded(emptyList(), null),
                                        error = CreateWalletError.Unknown(error.message)
                                    )
                                }
                            }
                    }
                }
            }
            is CreateWalletIntent.StateInputChanged -> updateState {
                require(it.stateInputState is StateInputState.Loaded)
                val newState = it.stateInputState.states.find { it.stateCode == intent.stateCode }!!
                it.copy(
                    stateInputState = it.stateInputState.copy(selected = newState)
                )
            }
            is CreateWalletIntent.ReferralInputChanged -> updateState {
                it.copy(referralCodeInput = intent.input, isInvalidReferralErrorShowing = false)
            }
            is CreateWalletIntent.TermsOfServiceStateChanged -> updateState {
                it.copy(areTermsOfServiceChecked = intent.isChecked)
            }
            CreateWalletIntent.NextClicked -> {
                analytics.logEventOnce(AnalyticsEvents.WalletSignupCreated)
                val validateInputsOutcome = modelState.validateInputs()
                if (validateInputsOutcome is Outcome.Failure) {
                    updateState { it.copy(error = validateInputsOutcome.failure) }
                    return
                }

                updateState { it.copy(isCreateWalletLoading = true) }
                if (modelState.referralCodeInput.isNotEmpty()) {
                    analytics.logEvent(ReferralAnalyticsEvents.ReferralCodeFilled(modelState.referralCodeInput))
                    val isReferralValidOutcome = referralService.isReferralCodeValid(modelState.referralCodeInput)
                    val isReferralValid = isReferralValidOutcome is Outcome.Success && isReferralValidOutcome.value
                    if (!isReferralValid) {
                        updateState { it.copy(isInvalidReferralErrorShowing = true, isCreateWalletLoading = false) }
                        return
                    }
                }

                payloadDataManager.createHdWallet(
                    modelState.passwordInput,
                    defaultLabels.getDefaultNonCustodialWalletLabel(),
                    modelState.emailInput
                ).awaitOutcome()
                    .doOnSuccess { wallet ->
                        val countryIso =
                            (modelState.countryInputState as CountryInputState.Loaded).selected!!.countryCode
                        val stateIso = (modelState.stateInputState as? StateInputState.Loaded)?.selected?.stateCode
                        analytics.logEvent(WalletCreationAnalytics.WalletSignUp(countryIso, stateIso))

                        walletStatusPrefs.apply {
                            isNewlyCreated = true
                            countrySelectedOnSignUp = countryIso
                            if (stateIso != null) stateSelectedOnSignUp = stateIso
                            email = modelState.emailInput
                        }

                        authPrefs.apply {
                            walletGuid = wallet.guid
                            sharedKey = wallet.sharedKey
                        }

                        analytics.logEvent(AnalyticsEvents.WalletCreation)
                        specificAnalytics.logSignUp(true)
                        navigate(CreateWalletNavigation.PinEntry(modelState.referralCodeInput.ifEmpty { null }))
                    }
                    .doOnFailure {
                        updateState {
                            it.copy(isCreateWalletLoading = false, error = CreateWalletError.WalletCreationFailed)
                        }
                        appUtil.clearCredentialsAndRestart()
                        specificAnalytics.logSignUp(false)
                    }
            }
            CreateWalletIntent.ErrorHandled -> updateState { it.copy(error = null) }
        }
    }

    private fun CreateWalletModelState.validateInputs(): Outcome<CreateWalletError, Unit> = when {
        !formatChecker.isValidEmailAddress(emailInput) -> Outcome.Failure(CreateWalletError.InvalidEmail)
        passwordInput.length < MIN_PWD_LENGTH -> Outcome.Failure(CreateWalletError.InvalidPasswordTooShort)
        passwordInput.length > MAX_PWD_LENGTH -> Outcome.Failure(CreateWalletError.InvalidPasswordTooLong)
        passwordInput != passwordConfirmationInput -> Outcome.Failure(CreateWalletError.PasswordsMismatch)
        !PasswordUtil.getStrength(passwordInput).roundToInt().isStrongEnough() ->
            Outcome.Failure(CreateWalletError.InvalidPasswordTooWeak)
        else -> Outcome.Success(Unit)
    }

    private fun Int.isStrongEnough(): Boolean {
        val limit = if (environmentConfig.isRunningInDebugMode()) 1 else 50
        return this >= limit
    }
}

private const val MIN_PWD_LENGTH = 4
private const val MAX_PWD_LENGTH = 255
private const val REFERRAL_CODE_LENGTH = 8
