package piuk.blockchain.android.ui.interest.domain

import org.koin.dsl.module
import piuk.blockchain.android.ui.interest.domain.usecase.GetAccountGroupUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetAssetInterestInfoUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDetailUseCase

val interestDashboardDomainModule = module {
    single { GetInterestDetailUseCase(service = get()) }
    single { GetAssetInterestInfoUseCase(service = get()) }
    single { GetAccountGroupUseCase(service = get()) }
}
