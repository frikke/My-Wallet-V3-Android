package piuk.blockchain.android.ui.interest.tbm.domain

import org.koin.dsl.module
import piuk.blockchain.android.ui.interest.tbm.domain.usecase.GetAssetInterestInfoUseCase
import piuk.blockchain.android.ui.interest.tbm.domain.usecase.GetInterestDetailUseCase

val interestDashboardDomainModule = module {
    single { GetInterestDetailUseCase(repository = get()) }
    single { GetAssetInterestInfoUseCase(repository = get()) }
}

