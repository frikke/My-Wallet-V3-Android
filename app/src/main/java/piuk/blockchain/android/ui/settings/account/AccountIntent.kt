package piuk.blockchain.android.ui.settings.account

import com.blockchain.commonarch.presentation.mvi.MviIntent
import info.blockchain.balance.FiatCurrency

sealed class AccountIntent : MviIntent<AccountState> {

    object LoadAccountInformation : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object LoadDisplayCurrencies : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object LoadTradingCurrencies : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object ResetViewState : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState.copy(
            viewToLaunch = ViewToLaunch.None
        )
    }

    object ToggleChartVibration : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    class UpdateChartVibration(private val isVibrationEnabled: Boolean) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState.copy(
            accountInformation = oldState.accountInformation?.copy(
                isChartVibrationEnabled = isVibrationEnabled
            )
        )
    }

    class UpdateViewToLaunch(private val viewToLaunch: ViewToLaunch) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState.copy(
            viewToLaunch = viewToLaunch
        )
    }

    class UpdateErrorState(private val accountError: AccountError) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState.copy(
            errorState = accountError
        )
    }

    class UpdateAccountInformation(private val accountInformation: AccountInformation) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState.copy(
            accountInformation = accountInformation
        )
    }

    class UpdateSelectedDisplayCurrency(val updatedCurrency: FiatCurrency) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    class UpdateSelectedTradingCurrency(val updatedCurrency: FiatCurrency) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object LoadFeatureFlags : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    class UpdateFeatureFlagSet(private val featureFlagSet: FeatureFlagSet) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState =
            oldState.copy(featureFlagSet = featureFlagSet)
    }
}
