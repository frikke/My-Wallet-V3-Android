package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money

data class CoinViewState(
    val asset: CryptoAsset? = null,
    val selectedFiat: FiatCurrency? = null,
    val selectedCryptoAccount: AssetDetailsItem.CryptoDetailsInfo? = null,
    val viewState: CoinViewViewState = CoinViewViewState.None,
    val assetDisplay: List<AssetDisplayInfo> = emptyList(),
    val error: CoinViewError = CoinViewError.None,
    val assetPrices: Prices24HrWithDelta? = null,
    val isAddedToWatchlist: Boolean = false,
    val canBuy: Boolean = false,
    val hasActionBuyWarning: Boolean = false
) : MviState

sealed class QuickActionCta(open val enabled: Boolean) {
    data class Buy(override val enabled: Boolean) : QuickActionCta(enabled)
    data class Sell(override val enabled: Boolean) : QuickActionCta(enabled)
    data class Send(override val enabled: Boolean) : QuickActionCta(enabled)
    data class Receive(override val enabled: Boolean) : QuickActionCta(enabled)
    data class Swap(override val enabled: Boolean) : QuickActionCta(enabled)
    object None : QuickActionCta(false)
}

data class QuickActionData(
    val middleAction: QuickActionCta,
    val startAction: QuickActionCta,
    val endAction: QuickActionCta,
    val actionableAccount: BlockchainAccount
)

sealed class AssetInformation(
    open val prices: Prices24HrWithDelta,
    open val isAddedToWatchlist: Boolean
) {
    data class AccountsInfo(
        override val isAddedToWatchlist: Boolean,
        override val prices: Prices24HrWithDelta,
        val accountsList: List<AssetDisplayInfo>,
        val totalCryptoBalance: Map<AssetFilter, Money>,
        val totalFiatBalance: Money
    ) : AssetInformation(prices, isAddedToWatchlist)

    class NonTradeable(
        override val isAddedToWatchlist: Boolean,
        override val prices: Prices24HrWithDelta,
    ) : AssetInformation(prices, isAddedToWatchlist)
}

/**
 * Model created from the interactor
 */
sealed class AssetDisplayInfo(
    open val account: BlockchainAccount,
    open val amount: Money,
    open val pendingAmount: Money,
    open val fiatValue: Money,
    open val actions: Set<StateAwareAction>,
    open val interestRate: Double,
    open val filter: AssetFilter
) {
    data class BrokerageDisplayInfo(
        override val account: BlockchainAccount,
        override val amount: Money,
        override val pendingAmount: Money,
        override val fiatValue: Money,
        override val actions: Set<StateAwareAction>,
        override val interestRate: Double,
        override val filter: AssetFilter
    ) : AssetDisplayInfo(
        account, amount, pendingAmount, fiatValue, actions, interestRate, filter
    )

    data class DefiDisplayInfo(
        override val account: BlockchainAccount,
        override val amount: Money,
        override val pendingAmount: Money,
        override val fiatValue: Money,
        override val actions: Set<StateAwareAction>,
    ) : AssetDisplayInfo(
        account, amount, pendingAmount, fiatValue, actions, Double.NaN, AssetFilter.NonCustodial
    )
}

class DetailsItem(
    val account: BlockchainAccount,
    val balance: Money,
    val pendingBalance: Money,
    val actions: Set<StateAwareAction>,
    val isDefault: Boolean = false
)
