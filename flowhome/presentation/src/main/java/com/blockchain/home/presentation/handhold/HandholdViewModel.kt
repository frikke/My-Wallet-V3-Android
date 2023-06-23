package com.blockchain.home.presentation.handhold

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.mapData
import com.blockchain.data.updateDataWith
import com.blockchain.home.handhold.HandholdService
import com.blockchain.home.handhold.isMandatory
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HandholdViewModel(
    private val handholdService: HandholdService,
    private val kycService: KycService,
    private val walletModeService: WalletModeService,
    private val dispatcher: CoroutineDispatcher
) : MviViewModel<HandholdIntent, HandholdViewState, HandholdModelState, EmptyNavEvent, ModelConfigArgs.NoArgs>(
    HandholdModelState()
) {

    init {
        viewModelScope.launch(dispatcher) {
            kycService.stateFor(KycTier.GOLD)
                .mapData {
                    it == KycTierState.Rejected
                }
                .collectLatest {
                    updateState {
                        copy(isKycRejected = it.dataOrElse(false))
                    }
                }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun HandholdModelState.reduce() = HandholdViewState(
        tasksStatus = data,
        showHandhold = walletMode?.let { walletMode ->
            data.map {
                walletMode == WalletMode.CUSTODIAL && it.any { it.task.isMandatory() && !it.isComplete }
            }
        } ?: DataResource.Loading,
        showKycRejected = isKycRejected
    )

    override suspend fun handleIntent(modelState: HandholdModelState, intent: HandholdIntent) {
        when (intent) {
            HandholdIntent.LoadData -> {
                loadData()
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch(dispatcher) {
            handholdService.handholdTasksStatus().collectLatest {
                updateState {
                    copy(data = data.updateDataWith(it))
                }
            }
        }

        viewModelScope.launch {
            walletModeService.walletMode.collectLatest {
                updateState {
                    copy(walletMode = it)
                }
            }
        }
    }
}
