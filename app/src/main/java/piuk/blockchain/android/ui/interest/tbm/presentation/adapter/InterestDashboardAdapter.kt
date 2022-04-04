package piuk.blockchain.android.ui.interest.tbm.presentation.adapter

import info.blockchain.balance.AssetInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.interest.InterestDashboardVerificationItem
import piuk.blockchain.android.ui.resources.AssetResources

class InterestDashboardAdapter(
    verificationClicked: () -> Unit,
    itemClicked: (AssetInfo, Boolean) -> Unit
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()), KoinComponent {

    private val assetResources: AssetResources by inject()

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(
                InterestDashboardAssetItem(
                    assetResources = assetResources,
                    itemClicked = itemClicked
                )
            )
            addAdapterDelegate(InterestDashboardVerificationItem(verificationClicked))
        }
    }
}

