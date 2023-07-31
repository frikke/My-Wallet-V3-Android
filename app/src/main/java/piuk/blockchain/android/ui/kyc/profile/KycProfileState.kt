package piuk.blockchain.android.ui.kyc.profile

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import java.util.Calendar

data class KycProfileModelState(
    val firstNameInput: String = "",
    val lastNameInput: String = "",
    val isNameInputErrorShowing: Boolean = false,
    val dateOfBirthInput: Calendar? = null,
    val isSavingProfileLoading: Boolean = false,
    val error: KycProfileError? = null
) : ModelState

data class KycProfileViewState(
    val firstNameInput: String,
    val lastNameInput: String,
    val isNameInputErrorShowing: Boolean,
    val dateOfBirthInput: Calendar?,
    val continueButtonState: ButtonState,
    val error: KycProfileError?
) : ViewState

sealed class KycProfileError {
    data class Generic(val message: String?) : KycProfileError()
    object UserConflict : KycProfileError()
    object InvalidName : KycProfileError()
}
