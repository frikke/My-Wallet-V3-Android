package piuk.blockchain.android.ui.customersupport

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel

class CustomerSupportViewModel(
    customerSupportModelState: CustomerSupportModelState
) : MviViewModel<CustomerSupportIntents,
    CustomerSupportViewState,
    CustomerSupportModelState,
    CustomerSupportNavigationEvent,
    ModelConfigArgs.NoArgs>(
    customerSupportModelState
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: CustomerSupportModelState): CustomerSupportViewState {
        return CustomerSupportViewState
    }

    override suspend fun handleIntent(modelState: CustomerSupportModelState, intent: CustomerSupportIntents) {
    }
}
