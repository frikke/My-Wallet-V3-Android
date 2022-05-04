package piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission

import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val bankAuthPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            OpenBankingPermissionViewModel(
                getSafeConnectTosLinkUseCase = payloadScope.get()
            )
        }
    }
}
