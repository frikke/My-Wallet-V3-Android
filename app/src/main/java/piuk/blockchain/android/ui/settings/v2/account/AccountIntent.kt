package piuk.blockchain.android.ui.settings.v2.account

import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class AccountIntent : MviIntent<AccountState> {

    object LoadAccountInformation : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object LoadExchangeInformation : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object LoadFiatList : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object LoadExchange : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object ResetViewState : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState.copy(
            viewToLaunch = ViewToLaunch.None
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

    class UpdateExchangeInformation(private val exchangeLinkingState: ExchangeLinkingState) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState.copy(
            exchangeLinkingState = exchangeLinkingState
        )
    }

    class UpdateFiatCurrency(val updatedCurrency: FiatCurrency) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }
}
