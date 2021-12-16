package piuk.blockchain.android.ui.launcher.loader

import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class LoaderIntents : MviIntent<LoaderState> {
    data class CheckIsLoggedIn(val isPinValidated: Boolean, val isAfterWalletCreation: Boolean) :
        LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState
    }

    object StartLauncherActivity : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState = oldState.copy(nextLoadingStep = LoadingStep.Launcher)
    }

    data class StartMainActivity(val data: String?, val launchDashboardOnboarding: Boolean) : LoaderIntents() {
        override fun reduce(oldState: LoaderState): LoaderState =
            oldState.copy(nextLoadingStep = LoadingStep.Main(data, launchDashboardOnboarding))
    }

    object OnEmailVerificationFinished : LoaderIntents() {
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
