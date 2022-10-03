package piuk.blockchain.android.ui.interest.presentation

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val interestDashboardPresentationModule = module {
    viewModel {
        InterestDashboardSharedViewModel()
    }

    scope(payloadScopeQualifier) {
        viewModel {
            InterestDashboardViewModel(
                kycService = get(),
                getInterestDashboardUseCase = get(),
                getAccountGroupUseCase = get()
            )
        }
    }
}
