package piuk.blockchain.android.ui.referral.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReferralViewModel : MviViewModel<ReferralIntents,
    ReferralViewState,
    ReferralModelState,
    ReferralNavigationEvent,
    ReferralArgs>(
    ReferralModelState()
) {

    private var resetCopyConfirmationDelayJob: Job? = null

    override fun viewCreated(args: ReferralArgs) {
        with(args) {
            updateState {
                it.copy(
                    code = code,
                    criteria = criteria,
                    rewardSubtitle = rewardSubtitle,
                    rewardTitle = rewardTitle
                )
            }
        }
    }

    override fun reduce(state: ReferralModelState): ReferralViewState {
        return with(state) {
            ReferralViewState(
                code = code,
                criteria = criteria,
                rewardSubtitle = rewardSubtitle,
                rewardTitle = rewardTitle,
                confirmCopiedToClipboard = confirmCopiedToClipboard
            )
        }
    }

    override suspend fun handleIntent(modelState: ReferralModelState, intent: ReferralIntents) {
        when (intent) {
            ReferralIntents.ConfirmCopiedToClipboard -> {
                updateState { it.copy(confirmCopiedToClipboard = true) }

                resetCopyConfirmationDelayJob?.cancel()
                resetCopyConfirmationDelayJob = viewModelScope.launch {
                    delay(COPY_CONFIRMATION_TIMEOUT)
                    updateState { it.copy(confirmCopiedToClipboard = false) }
                }
            }
        }.exhaustive
    }

    companion object {
        private const val COPY_CONFIRMATION_TIMEOUT = 3 * 1000L
    }
}
