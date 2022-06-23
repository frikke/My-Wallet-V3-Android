package piuk.blockchain.android.ui.dashboard.adapter

import com.blockchain.analytics.Analytics
import com.blockchain.coincore.FiatAccount
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.announcements.MiniAnnouncementDelegate
import piuk.blockchain.android.ui.dashboard.announcements.StdAnnouncementDelegate
import piuk.blockchain.android.ui.dashboard.model.Locks
import piuk.blockchain.android.ui.resources.AssetResources

class PortfolioDelegateAdapter(
    prefs: CurrencyPrefs,
    onCardClicked: (AssetInfo) -> Unit,
    onWalletModeChangeClicked: () -> Unit,
    analytics: Analytics,
    onFundsItemClicked: (FiatAccount) -> Unit,
    onHoldAmountClicked: (Locks) -> Unit,
    assetResources: AssetResources,
    walletModeService: WalletModeService,
    superAppFeatureFlag: FeatureFlag,
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(StdAnnouncementDelegate(analytics))
            addAdapterDelegate(FundsLockedDelegate(onHoldAmountClicked))
            addAdapterDelegate(MiniAnnouncementDelegate(analytics))
            addAdapterDelegate(
                BalanceCardDelegate(
                    prefs.selectedFiatCurrency,
                    assetResources,
                    walletModeService,
                    superAppFeatureFlag,
                    onWalletModeChangeClicked
                )
            )
            addAdapterDelegate(
                FundsCardDelegate(
                    prefs.selectedFiatCurrency,
                    onFundsItemClicked
                )
            )
            addAdapterDelegate(AssetCardDelegate(prefs, assetResources, onCardClicked))
            addAdapterDelegate(EmptyCardDelegate())
        }
    }
}
