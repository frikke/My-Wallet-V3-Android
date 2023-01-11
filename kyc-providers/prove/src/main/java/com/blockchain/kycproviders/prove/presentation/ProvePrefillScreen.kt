package com.blockchain.kycproviders.prove.presentation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.blockchain.addressverification.ui.AddressDetails
import com.blockchain.addressverification.ui.AddressVerificationHost
import com.blockchain.addressverification.ui.AddressVerificationScreen
import com.blockchain.addressverification.ui.Args
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.koin.payloadScope
import com.blockchain.kycproviders.prove.R
import com.blockchain.kycproviders.prove.presentation.screens.InstantLinkPhoneAndDobEntry
import com.blockchain.kycproviders.prove.presentation.screens.Intro
import com.blockchain.kycproviders.prove.presentation.screens.Loading
import com.blockchain.kycproviders.prove.presentation.screens.MobileAuthDobEntry
import com.blockchain.kycproviders.prove.presentation.screens.ViewPrefillData
import com.blockchain.kycproviders.prove.presentation.screens.WaitingInstantLinkValidation
import org.koin.androidx.compose.getViewModel

@Composable
fun ProvePrefillScreen(
    countryIso: CountryIso,
    stateIso: StateIso?,
    showDatePicker: () -> Unit,
    launchContactSupport: () -> Unit,
) {
    val viewModel: ProvePrefillModel = getViewModel(scope = payloadScope)

    val state by viewModel.viewState.collectAsStateLifecycleAware()
    val onIntent = viewModel::onIntent

    val addressVerificationHost = remember {
        object : AddressVerificationHost() {
            override fun launchContactSupport() {
                launchContactSupport()
            }

            override fun addressVerifiedSuccessfully(address: AddressDetails) {
                onIntent(ProvePrefillIntent.PrefillAddressEnteredSuccessfully(address))
            }
        }
    }

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) { padding ->
        LaunchedEffect(state.error) {
            val error = state.error
            if (error != null) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = error.errorMessage(context),
                    duration = SnackbarDuration.Long,
                )
            }
        }

        Box(Modifier.padding(padding)) {
            when (state.currentScreen) {
                Screen.INTRO -> Intro(state, onIntent)
                Screen.WAITING_MOBILE_AUTH_VALIDATION -> Loading()
                Screen.MOBILE_AUTH_DOB_ENTRY -> MobileAuthDobEntry(state, onIntent, showDatePicker)
                Screen.INSTANT_LINK_PHONE_AND_DOB_ENTRY ->
                    InstantLinkPhoneAndDobEntry(state, onIntent, showDatePicker)
                Screen.WAITING_INSTANT_LINK_VALIDATION -> WaitingInstantLinkValidation(state, onIntent)
                Screen.WAITING_PREFILL_DATA -> Loading(stringResource(R.string.prove_waiting_prefill_data_loading))
                Screen.VIEW_PREFILL_DATA -> ViewPrefillData(state, onIntent)
                Screen.MANUAL_ADDRESS_ENTRY -> AddressVerificationScreen(
                    Args(countryIso, stateIso, state.manualEntryAddress, true),
                    addressVerificationHost,
                )
                Screen.WAITING_PREFILL_DATA_SUBMISSION ->
                    Loading(stringResource(R.string.prove_waiting_prefill_submission_loading))
            }
        }
    }
}

internal val defaultViewState: ProvePrefillViewState = ProvePrefillViewState(
    currentScreen = Screen.INTRO,
    error = null,
    mobileNumberInput = "",
    dateOfBirthInput = null,
    possessionDataEntryContinueButtonState = ButtonState.Disabled,
    resendSmsButtonState = ButtonState.Disabled,
    resendSmsWaitTime = 0,
    prefillFirstNameInput = "",
    prefillLastNameInput = "",
    prefillSelectedAddress = null,
    prefillAddresses = emptyList(),
    manualEntryAddress = null,
    isAddressDropdownOpen = false,
    prefillDob = null,
    prefillMobileNumber = "",
    prefillContinueButtonState = ButtonState.Disabled,
)

private fun ProveError.errorMessage(context: Context): String = when (this) {
    is ProveError.Generic -> message ?: context.getString(R.string.something_went_wrong_try_again)
    ProveError.PossessionVerificationTimeout ->
        context.getString(R.string.prove_instant_link_phone_and_dob_entry_verification_timeout)
}
