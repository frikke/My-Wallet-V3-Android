package piuk.blockchain.android.ui.interest.data

import com.blockchain.koin.payloadScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import piuk.blockchain.android.ui.interest.data.repository.AssetInterestRepository
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService

val interestDashboardDataModule = module {
    single<AssetInterestService> {
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

