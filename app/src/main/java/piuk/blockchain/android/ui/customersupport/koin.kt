package piuk.blockchain.android.ui.customersupport

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val customerSupportModule = module {

    viewModel {
        CustomerSupportViewModel(
            customerSupportModelState = CustomerSupportModelState
        )
    }
}
