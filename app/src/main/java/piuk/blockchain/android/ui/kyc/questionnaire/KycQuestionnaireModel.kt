package piuk.blockchain.android.ui.kyc.questionnaire

import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.datamanagers.kyc.KycDataManager
import com.blockchain.nabu.datamanagers.kyc.SubmitQuestionnaireError
import com.blockchain.nabu.models.responses.nabu.NodeId
import com.blockchain.outcome.Outcome
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.campaign.CampaignType

data class KycQuestionnaireModelState(
    val nodes: List<FlatNode> = emptyList(),
    val isFormValid: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodes: List<NodeId> = emptyList(),
    val campaignType: CampaignType = CampaignType.None,
    val error: SubmitQuestionnaireError? = null
) : ModelState

sealed class Navigation : NavigationEvent {
    object LaunchVeriff : Navigation()
}

@Parcelize
data class Args(
    val root: TreeNode.Root,
    val campaignType: CampaignType
) : ModelConfigArgs.ParcelableArgs

class KycQuestionnaireModel(
    private val kycDataManager: KycDataManager,
    private val stateMachine: KycQuestionnaireStateMachine,
    private val analytics: Analytics
) : MviViewModel<
    KycQuestionnaireIntent,
    KycQuestionnaireState,
    KycQuestionnaireModelState,
    Navigation,
    Args
    >(KycQuestionnaireModelState()) {

    override fun viewCreated(args: Args) {
        analytics.logEvent(KycQuestionnaireViewed)
        updateState { it.copy(campaignType = args.campaignType) }
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

    override fun reduce(state: KycQuestionnaireModelState): KycQuestionnaireState = KycQuestionnaireState(
        nodes = state.nodes,
        isContinueEnabled = state.isFormValid,
        isUploadingNodes = state.isUploadingNodes,
        invalidNodesShown = state.invalidNodes,
        error = state.error
    )

    override suspend fun handleIntent(
        modelState: KycQuestionnaireModelState,
        intent: KycQuestionnaireIntent
    ) {
        when (intent) {
            is KycQuestionnaireIntent.DropdownChoiceChanged -> {
                stateMachine.onDropdownChoiceChanged(intent.node, intent.newChoice)
            }
            is KycQuestionnaireIntent.SelectionClicked -> {
                stateMachine.onSelectionClicked(intent.node)
            }
            is KycQuestionnaireIntent.OpenEndedInputChanged -> {
                stateMachine.onOpenEndedInputChanged(intent.node, intent.newInput)
            }
            KycQuestionnaireIntent.ContinueClicked -> {
                if (!modelState.isFormValid) {
                    updateState { it.copy(invalidNodes = stateMachine.invalidNodes) }
                    return
                }

                updateState { it.copy(isUploadingNodes = true, invalidNodes = emptyList()) }
                val nodes = stateMachine.getRoot().toDomain()
                when (val result = kycDataManager.submitQuestionnaire(nodes)) {
                    is Outcome.Success -> {
                        analytics.logEvent(KycQuestionnaireSubmitted)
                        navigate(Navigation.LaunchVeriff)
                    }
                    is Outcome.Failure -> updateState {
                        val invalidNodes =
                            listOfNotNull((result.failure as? SubmitQuestionnaireError.InvalidNode)?.nodeId)
                        it.copy(
                            isUploadingNodes = false,
                            error = result.failure,
                            invalidNodes = invalidNodes,
                            isFormValid = invalidNodes.isEmpty()
                        )
                    }
                }
            }
            KycQuestionnaireIntent.ErrorHandled -> updateState { it.copy(error = null) }
        }.exhaustive
    }
}
