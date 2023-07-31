package com.blockchain.home.presentation.onboarding.custodial

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.data.filterNotLoading
import com.blockchain.data.mapData
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.WalletStatusPrefs
import kotlinx.coroutines.flow.collectLatest

class CustodialIntroViewModel(
    private val walletStatusPrefs: WalletStatusPrefs,
    private val userFeaturePermissionService: UserFeaturePermissionService
) : MviViewModel<
    CustodialIntroIntent,
    CustodialIntroViewState,
    CustodialIntroModelState,
    CustodialIntroNavigationEvent,
    ModelConfigArgs.NoArgs> (initialState = CustodialIntroModelState(isEligibleForEarn = DataResource.Loading)) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) { }

    override fun CustodialIntroModelState.reduce() = CustodialIntroViewState(isEligibleForEarn = isEligibleForEarn)

    override suspend fun handleIntent(modelState: CustodialIntroModelState, intent: CustodialIntroIntent) {
        when (intent) {
            is CustodialIntroIntent.LoadData -> {
                userFeaturePermissionService.getAccessForFeatures(
                    Feature.DepositStaking,
                    Feature.DepositInterest,
                    Feature.DepositActiveRewards
                ).filterNotLoading().mapData {
                    it.any { (_, access) -> access is FeatureAccess.Granted }
                }.collectLatest {
                    updateState {
                        copy(isEligibleForEarn = it)
                    }
                }
            }
        }
    }

    fun markAsSeen() {
        walletStatusPrefs.hasSeenCustodialOnboarding = true
    }
}
sealed interface CustodialIntroIntent : Intent<CustodialIntroModelState> {
    object LoadData : CustodialIntroIntent
}

data class CustodialIntroModelState(val isEligibleForEarn: DataResource<Boolean>) : ModelState

data class CustodialIntroViewState(val isEligibleForEarn: DataResource<Boolean>) : ViewState

class CustodialIntroNavigationEvent : NavigationEvent
