package piuk.blockchain.android.ui.interest.tbm.presentation

import org.koin.dsl.module

val interestDashboardPresentationModule = module {
    single {
        InterestDashboardViewModel(
            getAssetInterestInfoUseCase = get(),
            getInterestDetailUseCase = get(),
            getAccountGroupUseCase = get()
        )
    }
}

