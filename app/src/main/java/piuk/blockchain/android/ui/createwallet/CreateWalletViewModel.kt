package piuk.blockchain.android.ui.createwallet

import androidx.lifecycle.viewModelScope
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.ProviderSpecificAnalytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.referral.ReferralService
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.getOrDefault
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.utils.awaitOutcome
import com.blockchain.wallet.DefaultLabels
import com.google.android.gms.recaptcha.RecaptchaActionType
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.referral.presentation.ReferralAnalyticsEvents
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.FormatChecker

enum class CreateWalletPasswordError {
    InvalidPasswordTooLong,
    InvalidPasswordTooShort,
    InvalidPasswordNoLowerCaseFound,
    InvalidPasswordNoUpperCaseFound,
    InvalidPasswordNoNumberFound,
    InvalidPasswordNoSpecialCharFound,
}

data class CreateWalletModelState(
    val screen: CreateWalletScreen = CreateWalletScreen.REGION_AND_REFERRAL,

    val emailInput: String = "",
    val isShowingInvalidEmailError: Boolean = false,

    val passwordInput: String = "",
    val passwordInputErrors: List<CreateWalletPasswordError> = emptyList(),
    val confirmPasswordInput: String = "",

    val countryInputState: CountryInputState = CountryInputState.Loading,
    val stateInputState: StateInputState = StateInputState.Hidden,

    val referralCodeInput: String = "",
    val isInvalidReferralErrorShowing: Boolean = false,

    val isCreateWalletLoading: Boolean = false,
    val isReferralValidationLoading: Boolean = false,

    val error: CreateWalletError? = null
) : ModelState {
    fun validateIsNextEnabled(step: CreateWalletScreen): Boolean = when (step) {
        CreateWalletScreen.REGION_AND_REFERRAL -> {
            countryInputState is CountryInputState.Loaded &&
                countryInputState.selected != null &&
                stateInputState.run {
                    this is StateInputState.Hidden || (this is StateInputState.Loaded && this.selected != null)
                } &&
                referralCodeInput.run {
                    this.isEmpty() || this.length == REFERRAL_CODE_LENGTH
                }
        }

        CreateWalletScreen.EMAIL_AND_PASSWORD -> {
            emailInput.isNotEmpty() &&
                passwordInput.isNotEmpty() &&
                passwordInputErrors.isEmpty() &&
                passwordInput == confirmPasswordInput
        }

        else -> true
    }
}

enum class CreateWalletScreen {
    REGION_AND_REFERRAL,
    EMAIL_AND_PASSWORD,
    CREATION_FAILED
}

sealed class CreateWalletNavigation : NavigationEvent {
    object Back : CreateWalletNavigation()
    data class RecaptchaVerification(val verificationType: String) : CreateWalletNavigation()
    data class PinEntry(val referralCode: String?) : CreateWalletNavigation()
}

sealed class CreateWalletError {
    object RecaptchaFailed : CreateWalletError()

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
    private val nabuUserDataManager: NabuUserDataManager
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
            val userGeolocationDeferred = async { nabuUserDataManager.getUserGeolocation().getOrDefault(null) }
            val countriesResult = eligibilityService.getCountriesList(GetRegionScope.Signup)
            val userGeolocation = userGeolocationDeferred.await()

            countriesResult
                .doOnSuccess { countries ->
                    val localisedCountries = countries.map {
                        val locale = Locale("", it.countryCode)
                        it.copy(name = locale.displayCountry)
                    }
                    val suggested = localisedCountries.find { it.countryCode == userGeolocation }
                    updateState {
                        copy(
                            countryInputState = CountryInputState.Loaded(
                                countries = localisedCountries,
                                selected = null,
                                suggested = suggested
                            )
                        )
                    }
                }
                .doOnFailure { error ->
                    updateState {
                        copy(
                            countryInputState = CountryInputState.Loaded(emptyList(), null, null),
                            error = CreateWalletError.Unknown(error.message)
                        )
                    }
                }
        }
    }

    override fun CreateWalletModelState.reduce() = CreateWalletViewState(
        screen = screen,
        emailInput = emailInput,
        isShowingInvalidEmailError = isShowingInvalidEmailError,
        passwordInput = passwordInput,
        passwordInputErrors = passwordInputErrors,
        countryInputState = countryInputState,
        stateInputState = stateInputState,
        referralCodeInput = referralCodeInput,
        isInvalidReferralErrorShowing = isInvalidReferralErrorShowing,
        isCreateWalletLoading = isCreateWalletLoading,
        nextButtonState = when {
            isCreateWalletLoading || isReferralValidationLoading -> ButtonState.Loading
            validateIsNextEnabled(screen) -> ButtonState.Enabled
            else -> ButtonState.Disabled
        },
        error = error
    )

    override suspend fun handleIntent(
        modelState: CreateWalletModelState,
        intent: CreateWalletIntent
    ) {
        when (intent) {
            is CreateWalletIntent.BackClicked -> when (modelState.screen) {
                CreateWalletScreen.REGION_AND_REFERRAL -> {
                    navigate(CreateWalletNavigation.Back)
                }

                CreateWalletScreen.EMAIL_AND_PASSWORD -> {
                    updateState { copy(screen = CreateWalletScreen.REGION_AND_REFERRAL) }
                }

                CreateWalletScreen.CREATION_FAILED -> {
                    updateState { copy(screen = CreateWalletScreen.EMAIL_AND_PASSWORD) }
                }
            }

            is CreateWalletIntent.EmailInputChanged -> {
                analytics.logEventOnce(AnalyticsEvents.WalletSignupClickEmail)
                updateState {
                    copy(emailInput = intent.input, isShowingInvalidEmailError = false)
                }
            }

            is CreateWalletIntent.PasswordInputChanged -> {
                analytics.logEventOnce(AnalyticsEvents.WalletSignupClickPasswordFirst)
                val passwordInputErrors: MutableList<CreateWalletPasswordError> = mutableListOf()
                if (intent.input.isNotEmpty()) {
                    passwordInputErrors.addAll(checkPasswordErrors(intent.input))
                }
                updateState {
                    copy(passwordInput = intent.input, passwordInputErrors = passwordInputErrors)
                }
            }

            is CreateWalletIntent.ConfirmPasswordInputChanged -> {
                updateState {
                    copy(confirmPasswordInput = intent.input)
                }
            }

            is CreateWalletIntent.CountryInputChanged -> {
                analytics.logEvent(WalletCreationAnalytics.CountrySelectedOnSignUp(intent.countryCode))
                fetchStatesJob?.cancel()
                require(modelState.countryInputState is CountryInputState.Loaded)
                val countryInputState = modelState.countryInputState
                val newCountry = countryInputState.countries.find { it.countryCode == intent.countryCode }!!

                val hasStates = newCountry.states.isNotEmpty()

                updateState {
                    copy(
                        countryInputState = countryInputState.copy(selected = newCountry),
                        stateInputState = if (!hasStates) StateInputState.Hidden else StateInputState.Loading
                    )
                }
                if (hasStates) {
                    fetchStatesJob = viewModelScope.launch {
                        eligibilityService.getStatesList(newCountry.countryCode, GetRegionScope.Signup)
                            .doOnSuccess { states ->
                                updateState { copy(stateInputState = StateInputState.Loaded(states, null)) }
                            }
                            .doOnFailure { error ->
                                updateState {
                                    copy(
                                        stateInputState = StateInputState.Loaded(emptyList(), null),
                                        error = CreateWalletError.Unknown(error.message)
                                    )
                                }
                            }
                    }
                }
            }

            is CreateWalletIntent.StateInputChanged -> updateState {
                analytics.logEvent(WalletCreationAnalytics.StateSelectedOnSignUp(intent.stateCode))
                require(stateInputState is StateInputState.Loaded)
                val newState = stateInputState.states.find { it.stateCode == intent.stateCode }!!
                copy(
                    stateInputState = stateInputState.copy(selected = newState)
                )
            }

            is CreateWalletIntent.ReferralInputChanged -> updateState {
                copy(referralCodeInput = intent.input, isInvalidReferralErrorShowing = false)
            }

            CreateWalletIntent.RegionNextClicked -> {
                if (modelState.referralCodeInput.isEmpty()) {
                    updateState { copy(screen = CreateWalletScreen.EMAIL_AND_PASSWORD) }
                } else {
                    updateState { copy(isReferralValidationLoading = true) }
                    analytics.logEvent(ReferralAnalyticsEvents.ReferralCodeFilled(modelState.referralCodeInput))
                    val isReferralValidOutcome = referralService.isReferralCodeValid(modelState.referralCodeInput)
                    val isReferralValid = isReferralValidOutcome is Outcome.Success && isReferralValidOutcome.value
                    if (isReferralValid) {
                        updateState {
                            copy(
                                isReferralValidationLoading = false,
                                screen = CreateWalletScreen.EMAIL_AND_PASSWORD
                            )
                        }
                    } else {
                        updateState {
                            copy(
                                isReferralValidationLoading = false,
                                isInvalidReferralErrorShowing = true
                            )
                        }
                    }
                }
            }

            is CreateWalletIntent.EmailPasswordNextClicked -> {
                analytics.logEventOnce(AnalyticsEvents.WalletSignupCreated)

                if (!formatChecker.isValidEmailAddress(modelState.emailInput)) {
                    updateState { copy(isShowingInvalidEmailError = true) }
                } else if (modelState.passwordInputErrors.isEmpty()) { // Is it worth checking this again??
                    navigate(CreateWalletNavigation.RecaptchaVerification(RecaptchaActionType.SIGNUP))
                }
            }

            is CreateWalletIntent.RecaptchaVerificationSucceeded -> {
                updateState { copy(isCreateWalletLoading = true) }

                payloadDataManager.createHdWallet(
                    modelState.passwordInput,
                    defaultLabels.getDefaultNonCustodialWalletLabel(),
                    modelState.emailInput,
                    intent.recaptchaToken
                ).awaitOutcome()
                    .doOnSuccess { wallet ->
                        val countryIso =
                            (modelState.countryInputState as CountryInputState.Loaded)
                                .selected!!.countryCode
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
                            copy(
                                isCreateWalletLoading = false,
                                screen = CreateWalletScreen.CREATION_FAILED,
                                emailInput = "",
                                passwordInput = ""
                            )
                        }
                        appUtil.clearCredentials()
                        specificAnalytics.logSignUp(false)
                    }
            }

            is CreateWalletIntent.RecaptchaVerificationFailed ->
                updateState { copy(error = CreateWalletError.RecaptchaFailed) }

            CreateWalletIntent.ErrorHandled ->
                updateState { copy(error = CreateWalletError.RecaptchaFailed) }
        }
    }

    private fun Double.isStrongEnough(): Boolean {
        val limit = if (environmentConfig.isRunningInDebugMode()) 1 else 50
        return this >= limit
    }

    private fun checkPasswordErrors(password: String): List<CreateWalletPasswordError> {
        return listOfNotNull(
            CreateWalletPasswordError.InvalidPasswordTooLong.takeIf {
                password.length > MAX_PWD_LENGTH
            },
            CreateWalletPasswordError.InvalidPasswordTooShort.takeIf {
                password.length < MIN_PWD_LENGTH
            },
            CreateWalletPasswordError.InvalidPasswordNoLowerCaseFound.takeIf {
                !password.any { it.isLowerCase() }
            },
            CreateWalletPasswordError.InvalidPasswordNoUpperCaseFound.takeIf {
                !password.any { it.isUpperCase() }
            },
            CreateWalletPasswordError.InvalidPasswordNoNumberFound.takeIf {
                !password.any { it.isDigit() }
            },
            CreateWalletPasswordError.InvalidPasswordNoSpecialCharFound.takeIf {
                !password.any { !it.isLetterOrDigit() }
            }
        )
    }
}

private const val MIN_PWD_LENGTH = 8
private const val MAX_PWD_LENGTH = 255
private const val REFERRAL_CODE_LENGTH = 8
