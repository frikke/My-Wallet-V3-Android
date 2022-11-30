package com.blockchain.home.presentation.referral

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.updateDataWith
import com.blockchain.domain.referral.ReferralService
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class ReferralViewModel(
    private val referralService: ReferralService
) : MviViewModel<
    ReferralIntent,
    ReferralViewState,
    ReferralModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(
    ReferralModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: ReferralModelState): ReferralViewState = state.run {
        ReferralViewState(
            referralInfo = referralInfo
        )
    }

    override suspend fun handleIntent(modelState: ReferralModelState, intent: ReferralIntent) {
        when (intent) {
            ReferralIntent.LoadData -> loadData()
        }
    }

    private suspend fun loadData() {
        referralService.fetchReferralData()
            .onEach { dataResource ->
                updateState {
                    it.copy(referralInfo = it.referralInfo.updateDataWith(dataResource))
                }
            }
            .collect()
    }
}