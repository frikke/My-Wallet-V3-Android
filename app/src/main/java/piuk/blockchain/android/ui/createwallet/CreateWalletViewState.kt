package piuk.blockchain.android.ui.createwallet

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.domain.eligibility.model.Region

data class CreateWalletViewState(
    val step: CreateWalletStep,

    val emailInput: String,
    val isShowingInvalidEmailError: Boolean,
    val passwordInput: String,
    val passwordInputError: CreateWalletPasswordError?,

    val countryInputState: CountryInputState,
    val stateInputState: StateInputState,

    val areTermsOfServiceChecked: Boolean,

    val referralCodeInput: String,
    val isInvalidReferralErrorShowing: Boolean,

    val isCreateWalletLoading: Boolean,
    val nextButtonState: ButtonState,

    val error: CreateWalletError? = null,
) : ViewState

sealed class CountryInputState {
    object Loading : CountryInputState()
    data class Loaded(
        val countries: List<Region.Country>,
        val selected: Region.Country?,
        val suggested: Region.Country?,
    ) : CountryInputState()
}

sealed class StateInputState {
    object Hidden : StateInputState()
    object Loading : StateInputState()
    data class Loaded(val states: List<Region.State>, val selected: Region.State?) : StateInputState()
}
