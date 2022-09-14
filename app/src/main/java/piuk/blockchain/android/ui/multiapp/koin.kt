package piuk.blockchain.android.ui.multiapp

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val multiAppModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            MultiAppViewModel(
                walletModeService = get()
            )
        }
    }
}
