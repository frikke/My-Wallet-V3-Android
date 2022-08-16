package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import piuk.blockchain.android.ui.interest.presentation.InterestDashboardSharedViewModel
import piuk.blockchain.android.ui.interest.presentation.InterestDashboardViewModel

val coinviewPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            CoinviewViewModel()
        }
    }
}
