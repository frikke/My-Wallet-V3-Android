package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.commonarch.presentation.mvi.MviState

sealed class LoadingStep {
    data class Main(val data: String?, val shouldLaunchUiTour: Boolean) : LoadingStep()
    class Error(val throwable: Throwable) : LoadingStep()
    object Launcher : LoadingStep()
    object EmailVerification : LoadingStep()
    object RequestPin : LoadingStep()
    object CowboysInterstitial : LoadingStep()
    data class EducationalWalletMode(val data: String?) : LoadingStep()
}

enum class ProgressStep {
    START, LOADING_PRICES, SYNCING_ACCOUNT, DECRYPTING_WALLET, FINISH
}

enum class ToastType {
    INVALID_PASSWORD, UNEXPECTED_ERROR
}

enum class LoginMethod {
    WALLET_CREATION, CREDENTIALS, PIN, UNDEFINED
}

data class LoaderState(
    val loginMethod: LoginMethod = LoginMethod.UNDEFINED,
    val nextProgressStep: ProgressStep? = null,
    val nextLoadingStep: LoadingStep? = null,
    val toastType: ToastType? = null,
    val shouldShowSecondPasswordDialog: Boolean = false,
    val shouldShowMetadataNodeFailure: Boolean = false,
    val isUserInCowboysPromo: Boolean = false
) : MviState
