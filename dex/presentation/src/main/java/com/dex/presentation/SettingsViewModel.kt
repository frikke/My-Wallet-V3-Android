package com.dex.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.dex.domain.DexTransactionProcessor
import com.dex.domain.SlippageService

class SettingsViewModel(
    private val slippageService: SlippageService,
    private val txProcessor: DexTransactionProcessor
) : MviViewModel<
    SettingsIntent,
    SettingsViewState,
    SettingsModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs
    >(initialState = SettingsModelState(emptyList())) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: SettingsModelState): SettingsViewState {
        return SettingsViewState(
            slippages = state.availableSlippages
        )
    }

    override suspend fun handleIntent(modelState: SettingsModelState, intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.LoadAvailableSlippages -> {
                val slippages = slippageService.availableSlippages()
                val selectedSlippage = slippageService.selectedSlippage()
                updateState {
                    it.copy(
                        availableSlippages = slippages.map { slp ->
                            Slippage(
                                factor = slp,
                                selected = selectedSlippage == slp
                            )
                        }
                    )
                }
            }
            is SettingsIntent.UpdateSelectedSlippage -> {
                slippageService.updateSelectedSlippageIndex(
                    modelState.availableSlippages.indexOfFirst {
                        it.factor == intent.slippage
                    }
                )
                txProcessor.updateSlippage(
                    intent.slippage
                )
            }
        }
    }
}

data class SettingsModelState(
    val availableSlippages: List<Slippage>,
) : ModelState

data class Slippage(
    val factor: Double,
    val selected: Boolean
)

data class SettingsViewState(val slippages: List<Slippage>) : ViewState

sealed class SettingsIntent : Intent<SettingsModelState> {
    object LoadAvailableSlippages : SettingsIntent()
    data class UpdateSelectedSlippage(val slippage: Double) : SettingsIntent()
}
