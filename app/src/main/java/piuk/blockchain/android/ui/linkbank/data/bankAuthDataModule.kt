package piuk.blockchain.android.ui.linkbank.presentation.yapily.permission.data

import org.koin.dsl.module
import piuk.blockchain.android.ui.linkbank.data.yapily.SafeConnectRepository
import piuk.blockchain.android.ui.linkbank.domain.yapily.SafeConnectService

val bankAuthDataModule = module {
    single<SafeConnectService> {
        SafeConnectRepository(
            remoteConfig = get()
        )
    }
}
