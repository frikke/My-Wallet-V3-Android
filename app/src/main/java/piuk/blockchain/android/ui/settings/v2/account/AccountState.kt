package piuk.blockchain.android.ui.settings.v2.account

import com.blockchain.commonarch.presentation.mvi.MviState
import info.blockchain.balance.FiatCurrency

data class AccountState(
    val viewToLaunch: ViewToLaunch = ViewToLaunch.None,
    val accountInformation: AccountInformation? = null,
    val errorState: AccountError = AccountError.NONE,
    val exchangeLinkingState: ExchangeLinkingState = ExchangeLinkingState.UNKNOWN
) : MviState

sealed class ViewToLaunch {
    object None : ViewToLaunch()
    class CurrencySelection(val selectedCurrency: FiatCurrency, val currencyList: List<FiatCurrency>) : ViewToLaunch()
    class ExchangeLink(val exchangeLinkingState: ExchangeLinkingState) : ViewToLaunch()
    class BcDebitCardState(val bcDebitCardState: DebitCardState) : ViewToLaunch()
}

enum class ExchangeLinkingState {
    UNKNOWN,
    NOT_LINKED,
    LINKED
}

enum class DebitCardState {
    NOT_ELIGIBLE,
    NOT_ORDERED,
    ORDERED
}

data class AccountInformation(
    val walletId: String,
    val userCurrency: FiatCurrency,
)

enum class AccountError {
    NONE,
    ACCOUNT_INFO_FAIL,
    FIAT_LIST_FAIL,
    ACCOUNT_FIAT_UPDATE_FAIL,
    EXCHANGE_INFO_FAIL,
    EXCHANGE_LOAD_FAIL,
    BLOCKCHAIN_CARD_LOAD_FAIL
}
