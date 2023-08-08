package piuk.blockchain.android.ui.kyc.veriffsplash

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import java.util.SortedSet

data class VeriffSplashModelState(
    val isLoading: Boolean = true,
    val supportedDocuments: SortedSet<SupportedDocuments> = sortedSetOf(),
    val error: VeriffSplashError? = null,
    val continueButtonState: ButtonState = ButtonState.Disabled
) : ModelState

data class VeriffSplashViewState(
    val isLoading: Boolean,
    val supportedDocuments: SortedSet<SupportedDocuments>,
    val error: VeriffSplashError?,
    val continueButtonState: ButtonState
) : ViewState

sealed class VeriffSplashError {
    object Generic : VeriffSplashError()
}
