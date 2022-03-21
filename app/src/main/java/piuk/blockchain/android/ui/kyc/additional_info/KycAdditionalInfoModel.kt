package piuk.blockchain.android.ui.kyc.additional_info

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.datamanagers.kyc.KycDataManager
import com.blockchain.nabu.datamanagers.kyc.UpdateKycAdditionalInfoError
import com.blockchain.nabu.models.responses.nabu.NodeId
import com.blockchain.outcome.Outcome
import java.lang.UnsupportedOperationException
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.ui.kyc.address.KycNextStepDecision
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

data class KycAdditionalInfoModelState(
    val nodes: List<FlatNode> = emptyList(),
    val isFormValid: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodes: List<NodeId> = emptyList(),
    val error: UpdateKycAdditionalInfoError? = null
) : ModelState

sealed class Navigation : NavigationEvent {
    object FinishWithSddComplete : Navigation()
    object FinishWithTier1Complete : Navigation()
    object LaunchVeriff : Navigation()
    object LaunchKycSplash : Navigation()
}

@Parcelize
data class Args(val root: TreeNode.Root) : ModelConfigArgs.ParcelableArgs

class KycAdditionalInfoModel(
    private val kycDataManager: KycDataManager,
    private val stateMachine: StateMachine,
    private val kycNextStepDecision: KycNextStepDecision
) : MviViewModel<
    KycAdditionalInfoIntent,
    KycAdditionalInfoState,
    KycAdditionalInfoModelState,
    Navigation,
    Args
    >(KycAdditionalInfoModelState()) {

    override fun viewCreated(args: Args) {
        stateMachine.onStateChanged = { nodes ->
            updateState { prevState ->
                prevState.copy(
                    nodes = nodes,
                    isFormValid = stateMachine.isValid(),
                    invalidNodes = emptyList() // we clear the errors after the user changes the form
                )
            }
        }
        stateMachine.setRoot(args.root)
    }

    override fun reduce(state: KycAdditionalInfoModelState): KycAdditionalInfoState = KycAdditionalInfoState(
        nodes = state.nodes,
        isContinueEnabled = state.isFormValid,
        isUploadingNodes = state.isUploadingNodes,
        invalidNodesShown = state.invalidNodes,
        error = state.error
    )

    override suspend fun handleIntent(
        modelState: KycAdditionalInfoModelState,
        intent: KycAdditionalInfoIntent
    ) {
        when (intent) {
            is KycAdditionalInfoIntent.DropdownChoiceChanged -> {
                stateMachine.onDropdownChoiceChanged(intent.node, intent.newChoice)
            }
            is KycAdditionalInfoIntent.SelectionClicked -> {
                stateMachine.onSelectionClicked(intent.node)
            }
            is KycAdditionalInfoIntent.OpenEndedInputChanged -> {
                stateMachine.onOpenEndedInputChanged(intent.node, intent.newInput)
            }
            KycAdditionalInfoIntent.ContinueClicked -> {
                if (!modelState.isFormValid) {
                    updateState { it.copy(invalidNodes = stateMachine.invalidNodes) }
                    return
                }

                updateState { it.copy(isUploadingNodes = true, invalidNodes = emptyList()) }
                val nodes = stateMachine.getRoot().toDomain()
                when (val result = kycDataManager.updateAdditionalInfo(nodes)) {
                    is Outcome.Success -> {
                        when (val result = kycNextStepDecision.nextStep().awaitOutcome()) {
                            is Outcome.Success -> navigate(result.value.toNavigation())
                            is Outcome.Failure -> updateState {
                                it.copy(
                                    isUploadingNodes = false,
                                    error = UpdateKycAdditionalInfoError.RequestFailed
                                )
                            }
                        }
                    }
                    is Outcome.Failure -> updateState {
                        val invalidNodes =
                            listOfNotNull((result.failure as? UpdateKycAdditionalInfoError.InvalidNode)?.nodeId)
                        it.copy(
                            isUploadingNodes = false,
                            error = result.failure,
                            invalidNodes = invalidNodes,
                            isFormValid = invalidNodes.isEmpty()
                        )
                    }
                }
            }
            KycAdditionalInfoIntent.ErrorHandled -> updateState { it.copy(error = null) }
        }.exhaustive
    }

    private fun KycNextStepDecision.NextStep.toNavigation(): Navigation = when (this) {
        is KycNextStepDecision.NextStep.MissingAdditionalInfo -> throw UnsupportedOperationException()
        KycNextStepDecision.NextStep.SDDComplete -> Navigation.FinishWithSddComplete
        KycNextStepDecision.NextStep.Tier1Complete -> Navigation.FinishWithTier1Complete
        KycNextStepDecision.NextStep.Tier2Continue -> Navigation.LaunchVeriff
        KycNextStepDecision.NextStep.Tier2ContinueTier1NeedsMoreInfo -> Navigation.LaunchKycSplash
    }
}
