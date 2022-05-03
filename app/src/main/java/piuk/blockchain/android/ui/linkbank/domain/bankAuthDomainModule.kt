package piuk.blockchain.android.ui.linkbank.domain

import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module
import piuk.blockchain.android.ui.linkbank.domain.yapily.usecase.GetSafeConnectTosLinkUseCase

val bankAuthDomainModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            GetSafeConnectTosLinkUseCase(
                service = get()
            )
        }
    }
}
