package piuk.blockchain.android.ui.dashboard.coinview.accounts

import com.blockchain.coincore.BlockchainAccount
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
    private val swapOnClick: (BlockchainAccount) -> Unit,
    private val onCardClicked: () -> Unit,
    private val onRecurringBuyClicked: (RecurringBuy) -> Unit,
    private val assetResources: AssetResources
) : DelegationAdapter<AssetDetailsItem>(AdapterDelegatesManager(), emptyList()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(
                BrokerageAccountDetailsDelegate(
                    onAccountSelected = onAccountSelected,
                    onLockedAccountSelected = onLockedAccountSelected,
                    labels = labels,
                    assetResources = assetResources
                )
            )
            addAdapterDelegate(
                DefiAccountDetailsDelegate(
                    onAccountSelected = onAccountSelected,
                    onLockedAccountSelected = onLockedAccountSelected
                )
            )
            addAdapterDelegate(CentralCtaDelegate(swapOnClick))
            addAdapterDelegate(RecurringBuyItemDelegate(onRecurringBuyClicked))
            addAdapterDelegate(RecurringBuyInfoItemDelegate(onCardClicked))
            addAdapterDelegate(RecurringBuyErrorDelegate())
            addAdapterDelegate(AccountErrorDelegate())
        }
    }
}
