package piuk.blockchain.android.ui.dataremediation

import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.NodeId
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError
import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.Outcome
import kotlinx.parcelize.Parcelize

data class QuestionnaireModelState(
    val nodes: List<FlatNode> = emptyList(),
    val isFormValid: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodes: List<NodeId> = emptyList(),
    val error: SubmitQuestionnaireError? = null
) : ModelState

sealed class Navigation : NavigationEvent {
    object LaunchVeriff : Navigation()
}

@Parcelize
data class Args(
    val root: TreeNode.Root
) : ModelConfigArgs.ParcelableArgs

class QuestionnaireModel(
    private val dataRemediationService: DataRemediationService,
    private val stateMachine: QuestionnaireStateMachine,
    private val analytics: Analytics
) : MviViewModel<
    QuestionnaireIntent,
    QuestionnaireState,
    QuestionnaireModelState,
    Navigation,
    Args
    >(QuestionnaireModelState()) {

    override fun viewCreated(args: Args) {
        analytics.logEvent(KycQuestionnaireViewed)
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

    override fun reduce(state: QuestionnaireModelState): QuestionnaireState = QuestionnaireState(
        nodes = state.nodes,
        isContinueEnabled = state.isFormValid,
        isUploadingNodes = state.isUploadingNodes,
        invalidNodesShown = state.invalidNodes,
        error = state.error
    )

    override suspend fun handleIntent(
        modelState: QuestionnaireModelState,
        intent: QuestionnaireIntent
    ) {
        when (intent) {
            is QuestionnaireIntent.DropdownChoiceChanged -> {
                stateMachine.onDropdownChoiceChanged(intent.node, intent.newChoice)
            }
            is QuestionnaireIntent.SelectionClicked -> {
                stateMachine.onSelectionClicked(intent.node)
            }
            is QuestionnaireIntent.OpenEndedInputChanged -> {
                stateMachine.onOpenEndedInputChanged(intent.node, intent.newInput)
            }
            QuestionnaireIntent.ContinueClicked -> {
                if (!modelState.isFormValid) {
                    updateState { it.copy(invalidNodes = stateMachine.invalidNodes) }
                    return
                }

                updateState { it.copy(isUploadingNodes = true, invalidNodes = emptyList()) }
                val nodes = stateMachine.getRoot().toDomain()
                when (val result = dataRemediationService.submitQuestionnaire(nodes)) {
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
            QuestionnaireIntent.ErrorHandled -> updateState { it.copy(error = null) }
        }.exhaustive
    }
}
