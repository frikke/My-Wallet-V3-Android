package piuk.blockchain.android.ui.linkbank.yapily.permission.data

import org.koin.dsl.module
import piuk.blockchain.android.ui.linkbank.yapily.permission.domain.SafeConnectRemoteConfig

val bankAuthDataModule = module {
    single<SafeConnectRemoteConfig> {
        SafeConnectRemoteConfigImpl(
            remoteConfig = get()
        )
    }
}
