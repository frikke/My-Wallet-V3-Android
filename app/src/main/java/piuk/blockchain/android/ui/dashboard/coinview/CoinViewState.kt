package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.nabu.models.data.RecurringBuy
import info.blockchain.balance.Money

data class CoinViewState(
    val asset: CryptoAsset? = null,
    val viewState: CoinViewViewState = CoinViewViewState.None,
    val assetDisplay: List<AssetDisplayInfo> = emptyList(),
    val error: CoinViewError = CoinViewError.None
) : MviState

sealed class CoinViewViewState {
    object None : CoinViewViewState()
    object Loading : CoinViewViewState()
    class ShowAccountInfo(val assetDisplay: List<AssetDisplayInfo>) : CoinViewViewState()
}
enum class CoinViewError {
    None
}

sealed class AssetDetailsItem {
    data class CryptoDetailsInfo(
        val assetFilter: AssetFilter,
        val account: BlockchainAccount,
        val balance: Money,
        val fiatBalance: Money,
        val actions: Set<AssetAction>,
        val interestRate: Double = Double.NaN
    ) : AssetDetailsItem()

    data class RecurringBuyInfo(
        val recurringBuy: RecurringBuy
    ) : AssetDetailsItem()

    object RecurringBuyBanner : AssetDetailsItem()
}

data class AssetDisplayInfo(
    val account: BlockchainAccount,
    val filter: AssetFilter,
    val amount: Money,
    val pendingAmount: Money,
    val fiatValue: Money,
    val actions: Set<StateAwareAction>,
    val interestRate: Double = Double.NaN
)

internal sealed class Details {
    object NoDetails : Details()
    class DetailsItem(
        val isEnabled: Boolean,
        val account: BlockchainAccount,
        val balance: Money,
        val pendingBalance: Money,
        val actions: Set<StateAwareAction>,
        val isDefault: Boolean = false
    ) : Details()
}
