package piuk.blockchain.android.ui.brokerage.sell

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.sell.domain.SellService
import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SellViewModel(
    private val sellService: SellService
) : MviViewModel<
    SellIntent,
    SellViewState,
    SellModelState,
    SellNavigation,
    ModelConfigArgs.NoArgs
    >(SellModelState(data = DataResource.Loading, shouldShowLoading = true)) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        viewModelScope.launch {
            sellService.loadSellAssets().collectLatest { data ->
                updateState {
                    it.copy(
                        data = data,
                        shouldShowLoading = false
                    )
                }
            }
        }
    }

    override fun reduce(state: SellModelState): SellViewState =
        SellViewState(data = state.data, showLoader = state.shouldShowLoading)

    override suspend fun handleIntent(modelState: SellModelState, intent: SellIntent) {
        when (intent) {
            is SellIntent.CheckSellEligibility -> {
                sellService.loadSellAssets().first()
            }
        }
    }
}
