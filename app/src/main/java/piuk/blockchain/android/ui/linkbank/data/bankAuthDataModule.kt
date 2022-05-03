package piuk.blockchain.android.ui.linkbank.data

import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module
import piuk.blockchain.android.ui.linkbank.data.yapily.SafeConnectRepository
import piuk.blockchain.android.ui.linkbank.domain.yapily.service.SafeConnectService

val bankAuthDataModule = module {
    scope(payloadScopeQualifier) {
        scoped <SafeConnectService> {
            SafeConnectRepository(
                remoteConfig = get()
            )
        }
    }
}
