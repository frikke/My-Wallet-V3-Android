package piuk.blockchain.android.ui.linkbank.domain

import org.koin.dsl.module
import piuk.blockchain.android.ui.linkbank.domain.yapily.usecase.GetSafeConnectTosLinkUseCase

val bankAuthDomainModule = module {
    single {
        GetSafeConnectTosLinkUseCase(
            service = get()
        )
    }
}
