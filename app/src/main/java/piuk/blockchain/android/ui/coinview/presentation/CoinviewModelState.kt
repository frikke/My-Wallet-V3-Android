package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.walletmode.WalletMode
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance

/**
 * @property assetPriceHistory - contains chart data + price and price change information
 * @property interactiveAssetPrice - price and price change information, used when user is interacting with the chart
 */
data class CoinviewModelState(
    val walletMode: WalletMode,

    val asset: CryptoAsset? = null,

    // price
    val isPriceDataLoading: Boolean = false,
    val isPriceDataError: Boolean = false,
    val assetPriceHistory: CoinviewAssetPriceHistory? = null,
    val requestedTimeSpan: HistoricalTimeSpan? = null,
    val interactiveAssetPrice: CoinviewAssetPrice? = null,

    // total balance
    val isTotalBalanceLoading: Boolean = false,
    val isTotalBalanceError: Boolean = false,
    val totalBalance: CoinviewAssetTotalBalance? = null,

    // accounts
    val isAccountsLoading: Boolean = false,
    val isAccountsError: Boolean = false,
    val accounts: CoinviewAccounts? = null
) : ModelState
