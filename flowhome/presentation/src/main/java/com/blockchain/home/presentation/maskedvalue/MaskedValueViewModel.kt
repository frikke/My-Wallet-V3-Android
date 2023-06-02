package com.blockchain.home.presentation.maskedvalue

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.mask.MaskedValueService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MaskedValueViewModel(
    private val maskedValueService: MaskedValueService
) : MviViewModel<
    MaskedValueIntent,
    MaskedValueViewState,
    MaskedValueModelState,
    EmptyNavEvent,
    ModelConfigArgs.NoArgs
    >(
    MaskedValueModelState(shouldMask = maskedValueService.shouldMask.value)
) {
    init {
        viewModelScope.launch {
            maskedValueService.shouldMask.collectLatest {
                updateState { copy(shouldMask = it) }
            }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun MaskedValueModelState.reduce() = MaskedValueViewState(
        isMaskActive = shouldMask
    )

    override suspend fun handleIntent(modelState: MaskedValueModelState, intent: MaskedValueIntent) {
        when (intent) {
            MaskedValueIntent.Toggle -> maskedValueService.toggleMaskState()
        }
    }
}

sealed interface MaskedValueIntent : Intent<MaskedValueModelState> {
    object Toggle : MaskedValueIntent
}

data class MaskedValueModelState(
    val shouldMask: Boolean
) : ModelState

data class MaskedValueViewState(
    val isMaskActive: Boolean
) : ViewState
