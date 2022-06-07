package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class LoaderIntents : MviIntent<LoaderState> {
    data class CheckIsLoggedIn(
        val isPinValidated: Boolean,
        val isAfterWalletCreation: Boolean,
        val referralCode: String?
    ) :
        LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState
    }

    object StartLauncherActivity : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(nextLoadingStep = LoadingStep.Launcher)
    }

    data class StartMainActivity(val data: String?, val shouldLaunchUiTour: Boolean) : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState =
            oldState.copy(nextLoadingStep = LoadingStep.Main(data, shouldLaunchUiTour))
    }

    object OnEmailVerificationFinished : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState
    }

    object OnTermsAndConditionsSigned : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState
    }

    data class DecryptAndSetupMetadata(val secondPassword: String) : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState
    }

    object ShowSecondPasswordDialog : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(shouldShowSecondPasswordDialog = true)
    }

    object HideSecondPasswordDialog : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(shouldShowSecondPasswordDialog = false)
    }

    object ShowMetadataNodeFailure : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(shouldShowMetadataNodeFailure = true)
    }

    object HideMetadataNodeFailure : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(shouldShowMetadataNodeFailure = false)
    }

    data class UpdateProgressStep(val progressStep: ProgressStep) : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(nextProgressStep = progressStep)
    }

    data class UpdateLoadingStep(val loadingStep: LoadingStep) : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(nextLoadingStep = loadingStep)
    }

    data class ShowToast(val toastType: ToastType) : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(toastType = toastType)
    }

    object ResetToast : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(toastType = null)
    }
}
