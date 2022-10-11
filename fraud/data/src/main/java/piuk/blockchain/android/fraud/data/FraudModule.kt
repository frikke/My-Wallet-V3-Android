package piuk.blockchain.android.fraud.data

import com.blockchain.api.interceptors.SessionId
import com.blockchain.koin.applicationScope
import com.blockchain.koin.sessionIdFeatureFlag
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.fraud.data.repository.FraudRepository
import piuk.blockchain.android.fraud.domain.service.FraudService

val fraudDataModule = module {
    single {
        FraudRepository(
            coroutineScope = get(applicationScope),
            dispatcher = Dispatchers.IO,
            sessionService = get(),
            sessionId = SessionId,
            sessionIdFeatureFlag = get(sessionIdFeatureFlag)
        )
    }.bind(FraudService::class)
}
