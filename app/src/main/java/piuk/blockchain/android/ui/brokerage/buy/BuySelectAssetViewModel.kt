package piuk.blockchain.android.ui.brokerage.buy

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.updateDataWith
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BuySelectAssetViewModel(
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val topMoversInBuyFF: FeatureFlag
) : MviViewModel<
    BuySelectAssetIntent,
    BuySelectAssetViewState,
    BuySelectAssetModelState,
    BuySelectAssetNavigation,
    ModelConfigArgs.NoArgs
    >(
    BuySelectAssetModelState()
) {
    init {
        viewModelScope.launch {
            val showTopMovers = topMoversInBuyFF.coEnabled()
            updateState {
                copy(showTopMovers = showTopMovers)
            }
        }
    }

    private var eligibilityJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun BuySelectAssetModelState.reduce() = BuySelectAssetViewState(
        featureAccess = featureAccess,
        showTopMovers = showTopMovers
    )

    override suspend fun handleIntent(
        modelState: BuySelectAssetModelState,
        intent: BuySelectAssetIntent
    ) {
        when (intent) {
            BuySelectAssetIntent.LoadEligibility -> {
                loadEligibility()
            }
            is BuySelectAssetIntent.AssetClicked -> {
                check(modelState.featureAccess is DataResource.Data)
                modelState.featureAccess.data.let {
                    val blockedReason = (it as? FeatureAccess.Blocked)?.reason
                    if (blockedReason is BlockedReason.TooManyInFlightTransactions) {
                        navigate(BuySelectAssetNavigation.PendingOrders(blockedReason.maxTransactions))
                    } else {
                        navigate(BuySelectAssetNavigation.SimpleBuy(intent.assetInfo))
                    }
                }
            }
        }
    }

    private fun loadEligibility() {
        eligibilityJob?.cancel()
        eligibilityJob = viewModelScope.launch {
            userFeaturePermissionService.getAccessForFeature(Feature.Buy)
                .collectLatest { dataResource ->
                    updateState {
                        copy(featureAccess = featureAccess.updateDataWith(dataResource))
                    }
                }
        }
    }
}
