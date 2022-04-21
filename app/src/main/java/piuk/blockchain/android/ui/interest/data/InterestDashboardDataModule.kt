package piuk.blockchain.android.ui.interest.data

import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import piuk.blockchain.android.ui.interest.data.repository.AssetInterestRepository
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService

val interestDashboardDataModule = module {
    scope(payloadScopeQualifier) {
        scoped<AssetInterestService> {
            AssetInterestRepository(
                kycTierService = payloadScope.get(),
                interestBalance = payloadScope.get(),
                custodialWalletManager = payloadScope.get(),
                exchangeRatesDataManager = get(),
                coincore = payloadScope.get(),
                dispatcher = Dispatchers.IO
            )
        }
    }
}
