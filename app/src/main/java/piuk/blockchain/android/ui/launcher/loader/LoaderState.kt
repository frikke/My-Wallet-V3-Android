package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.commonarch.presentation.mvi.MviState

sealed class LoadingStep {
    data class Main(val data: String?, val launchDashboardOnboarding: Boolean) : LoadingStep()
    class Error(val throwable: Throwable) : LoadingStep()
    object Launcher : LoadingStep()
    object EmailVerification : LoadingStep()
    object RequestPin : LoadingStep()
}

enum class ProgressStep {
    START, LOADING_PRICES, SYNCING_ACCOUNT, DECRYPTING_WALLET, FINISH
}

enum class ToastType {
    INVALID_PASSWORD, UNEXPECTED_ERROR
}

data class LoaderState(
    val nextProgressStep: ProgressStep? = null,
    val nextLoadingStep: LoadingStep? = null,
    val toastType: ToastType? = null,
    val shouldShowSecondPasswordDialog: Boolean = false,
    val shouldShowMetadataNodeFailure: Boolean = false
) : MviState
