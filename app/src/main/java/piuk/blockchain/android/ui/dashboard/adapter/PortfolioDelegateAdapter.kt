package piuk.blockchain.android.ui.dashboard.adapter

import androidx.recyclerview.widget.DiffUtil
import com.blockchain.analytics.Analytics
import com.blockchain.coincore.FiatAccount
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.announcements.CardAnnouncementDelegate
import piuk.blockchain.android.ui.dashboard.announcements.MiniAnnouncementDelegate
import piuk.blockchain.android.ui.dashboard.announcements.StdAnnouncementDelegate
import piuk.blockchain.android.ui.dashboard.model.DashboardItem
import piuk.blockchain.android.ui.dashboard.model.Locks
import piuk.blockchain.android.ui.resources.AssetResources

class PortfolioDelegateAdapter(
    prefs: CurrencyPrefs,
    onCardClicked: (AssetInfo) -> Unit,
    analytics: Analytics,
    walletModeService: WalletModeService,
    onFundsItemClicked: (FiatAccount) -> Unit,
    onHoldAmountClicked: (Locks) -> Unit,
    assetResources: AssetResources,
    assetCatalogue: AssetCatalogue
) : DelegationAdapter<DashboardItem>(AdapterDelegatesManager(), emptyList()) {

    override var items: List<DashboardItem> = emptyList()
        set(value) {
            val sorted = value.sortedBy { it.index }
            val diffResult =
                DiffUtil.calculateDiff(DashboardItemDiffUtil(this.items, sorted))
            field = sorted
            diffResult.dispatchUpdatesTo(this)
        }

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(StdAnnouncementDelegate(analytics))
            addAdapterDelegate(FundsLockedDelegate(onHoldAmountClicked))
            addAdapterDelegate(MiniAnnouncementDelegate(analytics))
            addAdapterDelegate(CardAnnouncementDelegate(analytics))
            addAdapterDelegate(
                BalanceCardDelegate(
                    walletModeService = walletModeService,
                    assetResources = assetResources,
                )
            )

            addAdapterDelegate(
                DefiBalanceDelegate()
            )
            addAdapterDelegate(
                FundsCardDelegate(
                    prefs.selectedFiatCurrency,
                    onFundsItemClicked
                )
            )
            addAdapterDelegate(DefiCardDelegate(assetResources, assetCatalogue, onCardClicked))
            addAdapterDelegate(BrokerageCardDelegate(prefs, assetResources, onCardClicked))
        }
    }
}

class DashboardItemDiffUtil(
    private val oldItems: List<DashboardItem>,
    private val newItems: List<DashboardItem>,
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldItems[oldItemPosition].id == newItems[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
