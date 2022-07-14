package piuk.blockchain.android.ui.dashboard.coinview.accounts

import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
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
    private val assetResources: AssetResources,
    private val walletMode: WalletMode
) : DelegationAdapter<AssetDetailsItem>(AdapterDelegatesManager(), emptyList()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDetailsDelegate(
                    onAccountSelected = onAccountSelected,
                    onLockedAccountSelected = onLockedAccountSelected,
                    labels = labels,
                    assetResources = assetResources,
                    allowWalletsLabel = walletMode != WalletMode.NON_CUSTODIAL_ONLY,
                    showOnlyAssetInfo = walletMode == WalletMode.NON_CUSTODIAL_ONLY,
                    allowEmbeddedCta = walletMode == WalletMode.NON_CUSTODIAL_ONLY
                )
            )
            addAdapterDelegate(RecurringBuyItemDelegate(onRecurringBuyClicked))
            addAdapterDelegate(RecurringBuyInfoItemDelegate(onCardClicked))
            addAdapterDelegate(RecurringBuyErrorDelegate())
            addAdapterDelegate(AccountErrorDelegate())
        }
    }
}
