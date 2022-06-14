package piuk.blockchain.android.ui.settings.v2.account

import com.blockchain.commonarch.presentation.mvi.MviIntent
import info.blockchain.balance.FiatCurrency

sealed class AccountIntent : MviIntent<AccountState> {

    object LoadAccountInformation : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object LoadFiatList : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    object LoadBCDebitCardInformation : AccountIntent() {
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

    class UpdateFiatCurrency(val updatedCurrency: FiatCurrency) : AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState
    }

    class UpdateBlockchainCardOrderState(private val blockchainCardOrderState: BlockchainCardOrderState) :
        AccountIntent() {
        override fun reduce(oldState: AccountState): AccountState = oldState.copy(
            blockchainCardOrderState = blockchainCardOrderState
        )
    }
}
