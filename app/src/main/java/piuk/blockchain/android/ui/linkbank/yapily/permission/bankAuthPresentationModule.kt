package piuk.blockchain.android.ui.linkbank.yapily.permission

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val bankAuthPresentationModule = module {
    viewModel {
        YapilyPermissionViewModel(
            safeConnectRemoteConfig = get(),
            downloadFileUseCase = get()
        )
    }
}
