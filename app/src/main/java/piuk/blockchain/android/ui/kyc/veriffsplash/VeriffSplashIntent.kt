package piuk.blockchain.android.ui.kyc.veriffsplash

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class VeriffSplashIntent : Intent<VeriffSplashModelState> {
    object OnVeriffSuccess : VeriffSplashIntent()
    data class OnVeriffFailure(val error: String?) : VeriffSplashIntent()
    object ContinueClicked : VeriffSplashIntent()
    object ErrorHandled : VeriffSplashIntent()
}
