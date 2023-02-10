package piuk.blockchain.android.fraud.data

import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.koin.applicationScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.fraud.data.repository.FraudRepository
import piuk.blockchain.android.fraud.domain.service.FraudFlows
import piuk.blockchain.android.fraud.domain.service.FraudService

val fraudDataModule = module {
    single {
        FraudRepository(
            coroutineScope = get(applicationScope),
            dispatcher = Dispatchers.IO,
            fraudService = get(),
            sessionInfo = SessionInfo,
            fraudFlows = FraudFlows,
            environmentConfig = get(),
        )
    }.bind(FraudService::class)
}
