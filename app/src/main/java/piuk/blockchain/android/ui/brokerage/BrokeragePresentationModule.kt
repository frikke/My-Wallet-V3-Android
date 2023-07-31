package piuk.blockchain.android.ui.brokerage

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.topMoversInBuy
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import piuk.blockchain.android.ui.brokerage.buy.BuySelectAssetViewModel

val brokeragePresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            BuySelectAssetViewModel(
                userFeaturePermissionService = get(),
                topMoversInBuyFF = get(topMoversInBuy)
            )
        }
    }
}
