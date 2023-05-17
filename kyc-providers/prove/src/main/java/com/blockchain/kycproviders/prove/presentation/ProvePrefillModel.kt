package com.blockchain.kycproviders.prove.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.addressverification.ui.AddressDetails
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.core.kyc.data.datasources.KycTiersStore
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.firstOutcome
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.Seconds
import com.blockchain.domain.common.model.StateIso
import com.blockchain.kycproviders.prove.data.ProveRepository
import com.blockchain.kycproviders.prove.domain.ProveService
import com.blockchain.kycproviders.prove.domain.model.Address
import com.blockchain.kycproviders.prove.domain.model.PossessionState
import com.blockchain.kycproviders.prove.domain.model.PrefillDataSubmission
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.util.toISO8601DateString
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

sealed class ProvePrefillIntent : Intent<ProvePrefillModelState> {
    object ErrorHandled : ProvePrefillIntent()
    object BackClicked : ProvePrefillIntent()

    // Intro
    object IntroContinueClicked : ProvePrefillIntent()

    // Phone and Dob Entry
    data class MobileNumberInputChanged(val newInput: String) : ProvePrefillIntent()
    data class DobInputChanged(val newInput: Calendar) : ProvePrefillIntent()
    object MobileAuthDobEntryContinueClicked : ProvePrefillIntent()
    object InstantLinkDataEntryContinueClicked : ProvePrefillIntent()

    // Waiting Instant Link Validation
    object ResendSmsClicked : ProvePrefillIntent()

    // View Prefill Data
    data class PrefillFirstNameInputChanged(val newInput: String) : ProvePrefillIntent()
    data class PrefillLastNameInputChanged(val newInput: String) : ProvePrefillIntent()
    data class PrefillAddressSelected(val address: AddressDetails) : ProvePrefillIntent()
    object PrefillAddressClicked : ProvePrefillIntent()
    object PrefillAddressDropdownClosed : ProvePrefillIntent()
    object PrefillAddressEnterManuallyClicked : ProvePrefillIntent()
    data class PrefillAddressEnteredSuccessfully(val addressDetails: AddressDetails) : ProvePrefillIntent()
    object PrefillContinueClicked : ProvePrefillIntent()
}

sealed class Navigation : NavigationEvent {
    object Back : Navigation()
    object ExitToProfileInfo : Navigation()
    object ExitToVeriff : Navigation()
    data class ExitToTierStatus(val kycState: KycState) : Navigation()
}

@Parcelize
data class Args(
    val countryIso: CountryIso,
    val stateIso: StateIso?
) : ModelConfigArgs.ParcelableArgs

class ProvePrefillModel(
    private val proveService: ProveService,
    private val userService: UserService,
    private val kycTiersStore: KycTiersStore
) : MviViewModel<
    ProvePrefillIntent,
    ProvePrefillViewState,
    ProvePrefillModelState,
    Navigation,
    Args
    >(ProvePrefillModelState()) {

    private lateinit var countryIso: CountryIso

    private lateinit var isMobileAuthAvailable: Deferred<Boolean>
    private lateinit var initialPossessionState: Deferred<Outcome<Exception, PossessionState>>

    override fun viewCreated(args: Args) {
        countryIso = args.countryIso
        isMobileAuthAvailable = viewModelScope.async {
            proveService.isMobileAuthPossible()
        }
        initialPossessionState = viewModelScope.async {
            proveService.getPossessionState()
        }
    }

    override fun ProvePrefillModelState.reduce(): ProvePrefillViewState {
        val possessionDataEntryContinueButtonState = when {
            isStartingInstantLinkAuthLoading -> ButtonState.Loading
            mobileNumberInput.isBlank() || dateOfBirthInput == null -> ButtonState.Disabled
            else -> ButtonState.Enabled
        }
        val resendSmsButtonState = if (resendSmsWaitTime == 0L) ButtonState.Enabled else ButtonState.Disabled
        val prefillContinueButtonState = if (
            prefillFirstNameInput.isBlank() ||
            prefillLastNameInput.isBlank() ||
            prefillSelectedAddress == null
        ) {
            ButtonState.Disabled
        } else {
            ButtonState.Enabled
        }

        return ProvePrefillViewState(
            currentScreen = currentScreen,
            error = error,
            mobileNumberInput = mobileNumberInput,
            dateOfBirthInput = dateOfBirthInput,
            possessionDataEntryContinueButtonState = possessionDataEntryContinueButtonState,
            resendSmsButtonState = resendSmsButtonState,
            resendSmsWaitTime = resendSmsWaitTime,
            prefillFirstNameInput = prefillFirstNameInput,
            prefillLastNameInput = prefillLastNameInput,
            prefillSelectedAddress = prefillSelectedAddress,
            prefillAddresses = prefillAddresses,
            manualEntryAddress = manualEntryAddress,
            isAddressDropdownOpen = isAddressDropdownOpen,
            prefillDob = prefillDob,
            prefillMobileNumber = prefillMobileNumber,
            prefillContinueButtonState = prefillContinueButtonState
        )
    }

    override suspend fun handleIntent(
        modelState: ProvePrefillModelState,
        intent: ProvePrefillIntent
    ) {
        when (intent) {
            ProvePrefillIntent.ErrorHandled -> updateState { copy(error = null) }
            ProvePrefillIntent.BackClicked -> {
                when (modelState.currentScreen) {
                    Screen.WAITING_INSTANT_LINK_VALIDATION -> updateState {
                        pollingForPossessionVerifiedJob?.cancel()
                        resendSmsWaitTimerJob?.cancel()
                        copy(currentScreen = Screen.INSTANT_LINK_PHONE_AND_DOB_ENTRY)
                    }

                    Screen.MANUAL_ADDRESS_ENTRY -> updateState {
                        copy(currentScreen = Screen.VIEW_PREFILL_DATA)
                    }

                    else -> navigate(Navigation.Back)
                }
            }

            ProvePrefillIntent.IntroContinueClicked -> {
                if (initialPossessionState.isActive || isMobileAuthAvailable.isActive) {
                    updateState {
                        copy(currentScreen = Screen.WAITING_MOBILE_AUTH_VALIDATION)
                    }
                }

                val possessionState = initialPossessionState.await()
                if (
                    possessionState is Outcome.Success &&
                    possessionState.value is PossessionState.Verified
                ) {
                    // if possession was already verified we ask for challenge data
                    updateState {
                        copy(
                            currentScreen = Screen.MOBILE_AUTH_DOB_ENTRY,
                            mobileNumberInput = (possessionState.value as PossessionState.Verified).mobileNumber
                        )
                    }
                } else if (isMobileAuthAvailable.await()) {
                    updateState {
                        copy(currentScreen = Screen.WAITING_MOBILE_AUTH_VALIDATION)
                    }
                    proveService.verifyPossessionWithMobileAuth()
                        .doOnSuccess { result ->
                            updateState {
                                copy(
                                    currentScreen = Screen.MOBILE_AUTH_DOB_ENTRY,
                                    mobileNumberInput = result.mobileNumber
                                )
                            }
                        }
                        .doOnFailure {
                            updateState { copy(currentScreen = Screen.INSTANT_LINK_PHONE_AND_DOB_ENTRY) }
                        }
                } else {
                    updateState { copy(currentScreen = Screen.INSTANT_LINK_PHONE_AND_DOB_ENTRY) }
                }
            }

            is ProvePrefillIntent.MobileNumberInputChanged -> updateState {
                val sanitized = intent.newInput.removePrefix("+1").filter(Char::isDigit).take(10)
                copy(mobileNumberInput = sanitized)
            }

            is ProvePrefillIntent.DobInputChanged -> updateState {
                copy(dateOfBirthInput = intent.newInput)
            }

            ProvePrefillIntent.MobileAuthDobEntryContinueClicked -> {
                val dob = modelState.dateOfBirthInput ?: return
                fetchAndShowPrefillData(dob)
            }

            ProvePrefillIntent.InstantLinkDataEntryContinueClicked -> {
                val mobileNumber = modelState.mobileNumberInput
                val dob = modelState.dateOfBirthInput ?: return
                updateState { copy(isStartingInstantLinkAuthLoading = true) }
                proveService.startInstantLinkAuth(mobileNumber)
                    .doOnSuccess {
                        startResendSmsWaitTimer(it.smsRetryInSeconds)
                        updateState {
                            copy(
                                currentScreen = Screen.WAITING_INSTANT_LINK_VALIDATION,
                                isStartingInstantLinkAuthLoading = false
                            )
                        }
                        startPollingForPossessionVerified(dob)
                    }
                    .doOnFailure { error ->
                        resendSmsWaitTimerJob?.cancel()
                        updateState {
                            copy(
                                error = error.toProveError(),
                                isStartingInstantLinkAuthLoading = false
                            )
                        }
                    }
            }

            ProvePrefillIntent.ResendSmsClicked -> {
                proveService.startInstantLinkAuth(modelState.mobileNumberInput)
                    .doOnSuccess {
                        startResendSmsWaitTimer(it.smsRetryInSeconds)
                    }.doOnFailure { error ->
                        updateState { copy(error = error.toProveError()) }
                    }
            }

            is ProvePrefillIntent.PrefillFirstNameInputChanged -> {
                updateState { copy(prefillFirstNameInput = intent.newInput) }
            }

            is ProvePrefillIntent.PrefillLastNameInputChanged -> {
                updateState { copy(prefillLastNameInput = intent.newInput) }
            }

            is ProvePrefillIntent.PrefillAddressSelected -> {
                updateState { copy(prefillSelectedAddress = intent.address, isAddressDropdownOpen = false) }
            }

            is ProvePrefillIntent.PrefillAddressClicked -> {
                val addresses = listOfNotNull(modelState.manualEntryAddress) + modelState.prefillAddresses
                if (addresses.size <= 1) {
                    if (modelState.prefillSelectedAddress == null) {
                        updateState { copy(currentScreen = Screen.MANUAL_ADDRESS_ENTRY) }
                    } else if (modelState.manualEntryAddress != null) {
                        updateState { copy(currentScreen = Screen.MANUAL_ADDRESS_ENTRY) }
                    } else {
                        // no op
                    }
                } else {
                    updateState { copy(isAddressDropdownOpen = true) }
                }
            }

            is ProvePrefillIntent.PrefillAddressDropdownClosed -> {
                updateState { copy(isAddressDropdownOpen = false) }
            }

            ProvePrefillIntent.PrefillAddressEnterManuallyClicked -> {
                updateState { copy(currentScreen = Screen.MANUAL_ADDRESS_ENTRY) }
            }

            is ProvePrefillIntent.PrefillAddressEnteredSuccessfully -> {
                updateState {
                    copy(
                        currentScreen = Screen.VIEW_PREFILL_DATA,
                        prefillSelectedAddress = intent.addressDetails,
                        manualEntryAddress = intent.addressDetails
                    )
                }
            }

            ProvePrefillIntent.PrefillContinueClicked -> {
                val firstName = modelState.prefillFirstNameInput
                val lastName = modelState.prefillLastNameInput

                val address = modelState.prefillSelectedAddress!!

                val provePrefill = PrefillDataSubmission(
                    firstName = firstName,
                    lastName = lastName,
                    address = address.toProveAddress(),
                    dob = modelState.prefillDob!!.toISO8601DateString(),
                    mobileNumber = modelState.prefillMobileNumber
                )

                updateState { copy(currentScreen = Screen.WAITING_PREFILL_DATA_SUBMISSION) }
                proveService.submitData(provePrefill)
                    .flatMap { userService.getUserResourceFlow(FreshnessStrategy.Fresh).firstOutcome() }
                    .doOnSuccess { user ->
                        kycTiersStore.markAsStale()
                        navigate(Navigation.ExitToTierStatus(user.kycState))
                    }
                    .doOnFailure { error ->
                        if (error is ProveRepository.VerificationWrongInfoException) {
                            navigate(Navigation.ExitToVeriff)
                        } else {
                            Timber.e(error)
                            updateState {
                                copy(
                                    currentScreen = Screen.VIEW_PREFILL_DATA,
                                    error = error.toProveError()
                                )
                            }
                        }
                    }
            }
        }
    }

    private var pollingForPossessionVerifiedJob: Job? = null
    private fun startPollingForPossessionVerified(dob: Calendar) {
        pollingForPossessionVerifiedJob?.cancel()
        pollingForPossessionVerifiedJob = viewModelScope.launch {
            proveService.pollForPossessionVerified()
                .doOnSuccess { possessionState ->
                    resendSmsWaitTimerJob?.cancel()

                    when (possessionState) {
                        PossessionState.Unverified -> updateState {
                            copy(
                                currentScreen = Screen.INSTANT_LINK_PHONE_AND_DOB_ENTRY,
                                error = ProveError.PossessionVerificationTimeout
                            )
                        }

                        is PossessionState.Verified -> fetchAndShowPrefillData(dob)
                        PossessionState.Failed -> navigate(Navigation.ExitToProfileInfo)
                    }
                }
                .doOnFailure { error ->
                    Timber.e(error)
                    resendSmsWaitTimerJob?.cancel()
                    updateState {
                        copy(
                            currentScreen = Screen.INSTANT_LINK_PHONE_AND_DOB_ENTRY,
                            error = error.toProveError()
                        )
                    }
                }
        }
    }

    private var resendSmsWaitTimerJob: Job? = null
    private fun startResendSmsWaitTimer(smsRetryInSeconds: Int) {
        resendSmsWaitTimerJob?.cancel()
        resendSmsWaitTimerJob = viewModelScope.launch {
            var time: Seconds = smsRetryInSeconds.toLong()
            updateState { copy(resendSmsWaitTime = time) }
            while (true) {
                delay(1_000)
                updateState { copy(resendSmsWaitTime = --time) }
                if (time <= 0L) cancel()
            }
        }
    }

    private fun fetchAndShowPrefillData(dob: Calendar) {
        viewModelScope.launch {
            updateState { copy(currentScreen = Screen.WAITING_PREFILL_DATA) }
            proveService.getPrefillData(dob.toISO8601DateString())
                .doOnSuccess { data ->
                    // TODO(aromano): PROVE distinguish between no data and error
                    val selectedAddress = if (data.addresses?.size == 1) {
                        data.addresses.first()
                    } else {
                        null
                    }
                    updateState {
                        copy(
                            currentScreen = Screen.VIEW_PREFILL_DATA,
                            prefillFirstNameInput = data.firstName.orEmpty(),
                            prefillLastNameInput = data.lastName.orEmpty(),
                            prefillAddresses = data.addresses.orEmpty().map { it.toAddressDetails(countryIso) },
                            prefillSelectedAddress = selectedAddress?.toAddressDetails(countryIso),
                            prefillDob = data.dob?.fromISO8601DataString() ?: dateOfBirthInput,
                            prefillMobileNumber = data.phoneNumber ?: mobileNumberInput
                        )
                    }
                }
                .doOnFailure {
                    // TODO(aromano): PROVE
                    updateState {
                        copy(
                            currentScreen = Screen.VIEW_PREFILL_DATA,
                            prefillMobileNumber = mobileNumberInput,
                            prefillDob = dateOfBirthInput
                        )
                    }
                }
        }
    }

    private fun String.fromISO8601DataString(): Calendar? {
        val backendFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateOfBirth = try {
            val date = backendFormat.parse(this)!!
            Calendar.getInstance().apply {
                time = date
            }
        } catch (ex: Exception) {
            null
        }
        return dateOfBirth
    }
}

private fun Address.toAddressDetails(countryIso: CountryIso): AddressDetails = AddressDetails(
    firstLine = line1,
    secondLine = line2,
    city = city,
    postCode = postCode,
    countryIso = countryIso,
    stateIso = state
)

private fun AddressDetails.toProveAddress(): Address = Address(
    line1 = firstLine,
    line2 = secondLine,
    city = city,
    state = stateIso.orEmpty(),
    postCode = postCode,
    country = countryIso
)
