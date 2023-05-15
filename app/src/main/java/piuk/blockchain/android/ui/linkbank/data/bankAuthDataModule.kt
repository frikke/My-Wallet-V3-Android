package piuk.blockchain.android.ui.linkbank.data

import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module
import piuk.blockchain.android.ui.linkbank.data.openbanking.SafeConnectRepository
import piuk.blockchain.android.ui.linkbank.domain.openbanking.service.SafeConnectService

val bankAuthDataModule = module {
    scope(payloadScopeQualifier) {
        scoped<SafeConnectService> {
            SafeConnectRepository(
                remoteConfigService = get()
            )
        }
    }
}
