package piuk.blockchain.android.ui.linkbank.presentation.yapily.permission

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val bankAuthPresentationModule = module {
    viewModel {
        YapilyPermissionViewModel(
            getSafeConnectTosLinkUseCase = get()
        )
    }
}
