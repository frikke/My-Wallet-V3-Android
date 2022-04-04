package piuk.blockchain.android.ui.interest.tbm.data

import com.blockchain.koin.payloadScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import piuk.blockchain.android.ui.interest.tbm.data.repository.AssetInterestRepositoryImpl
import piuk.blockchain.android.ui.interest.tbm.domain.repository.AssetInterestRepository

val interestDashboardDataModule = module {
    single<AssetInterestRepository> {
        AssetInterestRepositoryImpl(
            kycTierService = payloadScope.get(),
            interestBalance = payloadScope.get(),
            custodialWalletManager = payloadScope.get(),
            exchangeRatesDataManager = get(),
            dispatcher = Dispatchers.IO
        )
    }
}

