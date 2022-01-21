package piuk.blockchain.android.ui.settings.v2.security

import androidx.annotation.VisibleForTesting
import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class SecurityIntent : MviIntent<SecurityState> {

    object LoadInitialInformation : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState
    }

    class UpdateSecurityInfo(private val info: SecurityInfo) : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(securityInfo = info)
    }

    class UpdateErrorState(
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val error: SecurityError
    ) : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(errorState = error)
    }

    class UpdateViewState(
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val viewState: SecurityViewState
    ) : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(securityViewState = viewState)
    }

    object ResetViewState : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState =
            oldState.copy(securityViewState = SecurityViewState.None)
    }

    object ResetErrorState : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(errorState = SecurityError.NONE)
    }

    object ToggleBiometrics : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState
    }

    object DisableBiometrics : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState
    }

    object EnableBiometrics : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(
            securityInfo = oldState.securityInfo?.copy(isBiometricsEnabled = true)
        )
    }

    object BiometricsDisabled : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(
            securityInfo = oldState.securityInfo?.copy(isBiometricsEnabled = false)
        )
    }

    object ToggleTwoFa : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState
    }

    object TwoFactorEnabled : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(
            securityInfo = oldState.securityInfo?.copy(isTwoFaEnabled = true)
        )
    }

    object TwoFactorDisabled : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(
            securityInfo = oldState.securityInfo?.copy(isTwoFaEnabled = false)
        )
    }

    object ToggleScreenshots : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState
    }

    class UpdateScreenshotsEnabled(private val screenshotsEnabled: Boolean) : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(
            securityInfo = oldState.securityInfo?.copy(areScreenshotsEnabled = screenshotsEnabled)
        )
    }

    object ToggleTor : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState
    }

    class UpdateTorFiltering(private val filteringEnabled: Boolean) : SecurityIntent() {
        override fun reduce(oldState: SecurityState): SecurityState = oldState.copy(
            securityInfo = oldState.securityInfo?.copy(isTorFilteringEnabled = filteringEnabled)
        )
    }
}
