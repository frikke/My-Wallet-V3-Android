package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.nabu.models.data.RecurringBuy
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money

data class CoinViewState(
    val asset: CryptoAsset? = null,
    val selectedFiat: FiatCurrency? = null,
    val viewState: CoinViewViewState = CoinViewViewState.None,
    val assetDisplay: List<AssetDisplayInfo> = emptyList(),
    val error: CoinViewError = CoinViewError.None,
    val assetPrices: Prices24HrWithDelta? = null
) : MviState

sealed class CoinViewViewState {
    object None : CoinViewViewState()
    object LoadingWallets : CoinViewViewState()
    object LoadingChart : CoinViewViewState()
    class ShowAccountInfo(val assetInfo: AssetInformation) : CoinViewViewState()
    class ShowAssetInfo(
        val entries: List<ChartEntry>,
        val prices: Prices24HrWithDelta,
        val historicalRateList: HistoricalRateList,
        val selectedFiat: FiatCurrency
    ) : CoinViewViewState()
}

enum class CoinViewError {
    None,
    UnknownAsset,
    WalletLoadError,
    ChartLoadError
}

data class AssetInformation(
    val prices: Prices24HrWithDelta,
    val accountsList: List<AssetDisplayInfo>,
    val totalCryptoBalance: CryptoValue,
    val totalFiatBalance: FiatValue
)

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
