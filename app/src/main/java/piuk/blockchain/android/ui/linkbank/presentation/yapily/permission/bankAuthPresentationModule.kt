package piuk.blockchain.android.ui.linkbank.presentation.yapily.permission

import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val bankAuthPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            YapilyPermissionViewModel(
                getSafeConnectTosLinkUseCase = payloadScope.get()
            )
        }
    }
}
