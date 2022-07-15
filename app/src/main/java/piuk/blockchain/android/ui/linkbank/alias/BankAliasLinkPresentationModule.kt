package piuk.blockchain.android.ui.linkbank.alias

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val bankAliasLinkPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            BankAliasLinkViewModel(
                bankService = get()
            )
        }
    }
}
