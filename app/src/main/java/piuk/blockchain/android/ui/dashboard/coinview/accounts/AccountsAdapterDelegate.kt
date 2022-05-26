package piuk.blockchain.android.ui.dashboard.coinview.accounts

import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.wallet.DefaultLabels
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.coinview.AssetDetailsItem
import piuk.blockchain.android.ui.resources.AssetResources

class AccountsAdapterDelegate(
    private val onAccountSelected: (AssetDetailsItem.CryptoDetailsInfo) -> Unit,
    private val onLockedAccountSelected: () -> Unit,
    private val labels: DefaultLabels,
    private val onCardClicked: () -> Unit,
    private val onRecurringBuyClicked: (RecurringBuy) -> Unit,
    private val assetResources: AssetResources
) : DelegationAdapter<AssetDetailsItem>(AdapterDelegatesManager(), emptyList()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDetailsDelegate(
                    onAccountSelected,
                    onLockedAccountSelected,
                    labels,
                    assetResources
                )
            )
            addAdapterDelegate(RecurringBuyItemDelegate(onRecurringBuyClicked))
            addAdapterDelegate(RecurringBuyInfoItemDelegate(onCardClicked))
            addAdapterDelegate(RecurringBuyErrorDelegate())
            addAdapterDelegate(AccountErrorDelegate())
        }
    }
}
