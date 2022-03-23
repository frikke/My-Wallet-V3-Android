package piuk.blockchain.android.ui.pinhelp

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val pinHelpModuleModule = module {

    viewModel {
        PinHelpViewModel(
            loginHelpModelState = PinHelpModelState
        )
    }
}
