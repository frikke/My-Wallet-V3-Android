package piuk.blockchain.android.ui.interest.domain

import org.koin.dsl.module
import piuk.blockchain.android.ui.interest.domain.usecase.GetAccountGroupUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetAssetsInterestUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDashboardUseCase

val interestDashboardDomainModule = module {
    single { GetInterestDashboardUseCase(service = get()) }
    single { GetAssetsInterestUseCase(service = get()) }
    single { GetAccountGroupUseCase(service = get()) }
}
