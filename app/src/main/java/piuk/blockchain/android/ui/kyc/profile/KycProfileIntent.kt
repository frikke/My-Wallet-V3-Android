package piuk.blockchain.android.ui.kyc.profile

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import java.util.Calendar

sealed class KycProfileIntent : Intent<KycProfileModelState> {
    data class FirstNameInputChanged(val value: String) : KycProfileIntent()
    data class LastNameInputChanged(val value: String) : KycProfileIntent()
    data class DateOfBirthInputChanged(val value: Calendar) : KycProfileIntent()
    object ContinueClicked : KycProfileIntent() {
        override fun isValidFor(modelState: KycProfileModelState): Boolean =
            !modelState.isSavingProfileLoading
    }
    object ErrorHandled : KycProfileIntent()
}
