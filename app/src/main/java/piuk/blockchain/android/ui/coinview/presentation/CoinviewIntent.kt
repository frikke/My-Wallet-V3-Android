package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.walletmode.WalletMode
import com.github.mikephil.charting.data.Entry
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction

sealed interface CoinviewIntent : Intent<CoinviewModelState> {
    /**
     * Triggers loading:
     * * asset price / chart values
     * * asset accounts
     * * recurring buys
     * * todo
     */
    object LoadAllData : CoinviewIntent

    /**
     * Load asset price and chart data
     */
    object LoadPriceData : CoinviewIntent

    object LoadWatchlistData : CoinviewIntent

    /**
     * Load total balance and accounts
     */
    object LoadAccountsData : CoinviewIntent

    /**
     * Load recurring buys / show upsell when no data (if eligible)
     * Not supported by [WalletMode.NON_CUSTODIAL_ONLY]
     */
    object LoadRecurringBuysData : CoinviewIntent {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.walletMode != WalletMode.NON_CUSTODIAL_ONLY
        }
    }

    /**
     * Load quick actions to setup the center and bottom buttons
     *
     * Should only load when accounts are already loaded
     * todo(othman) remove this check once accounts are cached
     * Should only load when balances are already loaded
     * todo(othman) remove this check once accounts are cached
     */
    object LoadQuickActions : CoinviewIntent {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.accounts != null && modelState.totalBalance != null
        }
    }

    /**
     * Load asset description / website
     */
    object LoadAssetInfo : CoinviewIntent

    /**
     * Performs price updates while chart is interactive
     */
    data class UpdatePriceForChartSelection(val entry: Entry) : CoinviewIntent {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.assetPriceHistory?.historicRates?.isNotEmpty() ?: false
        }
    }

    /**
     * Reset price to original value after chart interaction
     */
    object ResetPriceSelection : CoinviewIntent

    /**
     * Load a new time span chart
     */
    data class NewTimeSpanSelected(val timeSpan: HistoricalTimeSpan) : CoinviewIntent {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.assetPriceHistory?.priceDetail?.timeSpan != timeSpan
        }
    }

    object ToggleWatchlist : CoinviewIntent {
        override fun isValidFor(modelState: CoinviewModelState): Boolean {
            return modelState.watchlist != null && modelState.isWatchlistLoading.not()
        }
    }

    /**
     * Account selected
     * could either open an explainer sheet or the actions if the explainer was previously seen
     * @see LockedAccountSelected
     */
    data class AccountSelected(val account: CoinviewAccount) : CoinviewIntent

    /**
     * User confirm the account explainer - now show the account actions
     *
     *todo support [CoinviewAccount]
     * So far the coinview bottom sheets are from the original implementation
     * they also need to be refactored to suit the new one
     * so we still need to get the account: BlockchainAccount to open the actions sheet
     */
    data class AccountExplainerAcknowledged(
        val account: BlockchainAccount,
        val actions: List<StateAwareAction>
    ) : CoinviewIntent

    data class AccountActionSelected(
        val account: BlockchainAccount,
        val action: AssetAction
    ) : CoinviewIntent

    /**
     * When selecting e.g. withdraw with zero balance
     */
    data class NoBalanceUpsell(
        val account: BlockchainAccount,
        val action: AssetAction
    ) : CoinviewIntent

    /**
     * A locked account was selected
     * @see AccountSelected For normal account
     */
    object LockedAccountSelected : CoinviewIntent

    /**
     * If the asset has no recurring buy configured
     */
    object RecurringBuysUpsell : CoinviewIntent

    data class ShowRecurringBuyDetail(val recurringBuyId: String) : CoinviewIntent

    /**
     * User clicked on one of the quick action buttons (buy/sell/send/receive/swap)
     * It uses [CoinviewModelState.actionableAccount] as a target account
     */
    data class QuickActionSelected(val quickAction: CoinviewQuickAction) : CoinviewIntent

    object ContactSupport : CoinviewIntent
}
