package piuk.blockchain.android.ui.interest.presentation

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val interestDashboardPresentationModule = module {
    viewModel {
        InterestDashboardViewModel(
            getAssetInterestInfoUseCase = get(),
            getInterestDetailUseCase = get(),
            getAccountGroupUseCase = get()
        )
    }

    viewModel {
        InterestDashboardSharedViewModel()
    }
}
