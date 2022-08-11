package piuk.blockchain.android.ui.interest.domain

import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module
import piuk.blockchain.android.ui.interest.domain.usecase.GetAccountGroupUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetAssetsInterestUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDashboardUseCase

val interestDashboardDomainModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            GetInterestDashboardUseCase(
                interestService = get(),
                exchangeRatesDataManager = get()
            )
        }
        scoped { GetAssetsInterestUseCase(service = get()) }
        scoped { GetAccountGroupUseCase(service = get()) }
    }
}
