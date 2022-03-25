package piuk.blockchain.android.ui.dashboard.coinview.accounts

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.wallet.DefaultLabels
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItemNew
import piuk.blockchain.android.ui.dashboard.assetdetails.delegates.AssetDetailsInfoDecorator
import piuk.blockchain.android.ui.resources.AssetResources

class AccountsAdapterDelegate(
    private val onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit,
    private val labels: DefaultLabels,
    private val onCardClicked: () -> Unit,
    private val onRecurringBuyClicked: (RecurringBuy) -> Unit,
    private val assetDetailsDecorator: AssetDetailsInfoDecorator,
    private val assetResources: AssetResources
) : DelegationAdapter<AssetDetailsItemNew>(AdapterDelegatesManager(), emptyList()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDetailsDelegate(
                    onAccountSelected,
                    compositeDisposable,
                    assetDetailsDecorator,
                    labels,
                    assetResources
                )
            )
            addAdapterDelegate(RecurringBuyItemDelegate(onRecurringBuyClicked))
            addAdapterDelegate(RecurringBuyInfoItemDelegate(onCardClicked))
        }
    }
}
