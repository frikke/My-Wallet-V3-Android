package piuk.blockchain.android.ui.settings.v2.account

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.commonarch.presentation.mvi.MviState
import info.blockchain.balance.FiatCurrency

data class AccountState(
    val viewToLaunch: ViewToLaunch = ViewToLaunch.None,
    val accountInformation: AccountInformation? = null,
    val errorState: AccountError = AccountError.NONE,
    val exchangeLinkingState: ExchangeLinkingState = ExchangeLinkingState.UNKNOWN,
    val blockchainCardOrderState: BlockchainCardOrderState = BlockchainCardOrderState.NotEligible
) : MviState

sealed class ViewToLaunch {
    object None : ViewToLaunch()
    class CurrencySelection(val selectedCurrency: FiatCurrency, val currencyList: List<FiatCurrency>) : ViewToLaunch()
}

enum class ExchangeLinkingState {
    UNKNOWN,
    NOT_LINKED,
    LINKED
}

sealed class BlockchainCardOrderState {
    object NotEligible : BlockchainCardOrderState()
    data class Eligible(val cardProducts: List<BlockchainCardProduct>) : BlockchainCardOrderState()
    data class Ordered(val blockchainCard: BlockchainCard) : BlockchainCardOrderState()
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
    BLOCKCHAIN_CARD_LOAD_FAIL
}
