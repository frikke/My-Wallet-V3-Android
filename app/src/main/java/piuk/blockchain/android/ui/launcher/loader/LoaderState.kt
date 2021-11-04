package piuk.blockchain.android.ui.launcher.loader

import piuk.blockchain.android.ui.base.mvi.MviState

sealed class LoaderStep {
    data class Main(val data: String?, val launchBuySellIntro: Boolean) : LoaderStep()
    object Launcher : LoaderStep()
    object EmailVerification : LoaderStep()
    object RequestPin : LoaderStep()
}

enum class ProgressStep {
    START, LOADING_PRICES, SYNCING_ACCOUNT, DECRYPTING_WALLET, FINISH
}

enum class ToastType {
    INVALID_PASSWORD, UNEXPECTED_ERROR
}

data class LoaderState(
    val nextProgressStep: ProgressStep? = null,
    val nextLoaderStep: LoaderStep? = null,
    val toastType: ToastType? = null,
    val shouldShowSecondPasswordDialog: Boolean = false,
    val shouldShowMetadataNodeFailure: Boolean = false
) : MviState
