package piuk.blockchain.android.ui.dataremediation

import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.NodeId
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError
import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.Outcome
import kotlinx.parcelize.Parcelize

data class QuestionnaireModelState(
    val nodes: List<FlatNode> = emptyList(),
    val isFormValid: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodes: List<NodeId> = emptyList(),
    val error: QuestionnaireError? = null
) : ModelState

sealed class Navigation : NavigationEvent {
    data class OpenDropdownPickerForSingleSelection(
        val original: FlatNode.Dropdown,
        val node: TreeNode.SingleSelection
    ) : Navigation()

    data class OpenDropdownPickerForMultipleSelection(
        val original: FlatNode.Dropdown,
        val node: TreeNode.MultipleSelection
    ) : Navigation()

    object FinishSuccessfully : Navigation()
}

@Parcelize
data class Args(
    val questionnaire: Questionnaire
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

    private lateinit var originalQuestionnaire: Questionnaire

    override fun viewCreated(args: Args) {
        analytics.logEvent(KycQuestionnaireViewed)
        originalQuestionnaire = args.questionnaire
        stateMachine.onStateChanged = { nodes ->
            updateState {
                copy(
                    nodes = nodes,
                    isFormValid = stateMachine.isValid(),
                    invalidNodes = emptyList() // we clear the errors after the user changes the form
                )
            }
        }
        stateMachine.setRoot(args.questionnaire.nodes.toMutableNode())
    }

    override fun QuestionnaireModelState.reduce() = QuestionnaireState(
        nodes = nodes,
        isContinueEnabled = isFormValid,
        isUploadingNodes = isUploadingNodes,
        invalidNodesShown = invalidNodes,
        error = error
    )

    override suspend fun handleIntent(
        modelState: QuestionnaireModelState,
        intent: QuestionnaireIntent
    ) {
        when (intent) {
            is QuestionnaireIntent.DropdownOpenPickerClicked -> {
                val treeNode = stateMachine.getTreeNode(intent.node)
                if (treeNode is TreeNode.SingleSelection) {
                    navigate(Navigation.OpenDropdownPickerForSingleSelection(intent.node, treeNode))
                } else if (treeNode is TreeNode.MultipleSelection) {
                    navigate(Navigation.OpenDropdownPickerForMultipleSelection(intent.node, treeNode))
                } else {
                    throw IllegalStateException()
                }
            }

            is QuestionnaireIntent.DropdownChoicesChanged -> {
                stateMachine.onDropdownChoicesChanged(intent.node, intent.newChoices)
            }

            is QuestionnaireIntent.SelectionClicked -> {
                stateMachine.onSelectionClicked(intent.node)
            }

            is QuestionnaireIntent.OpenEndedInputChanged -> {
                stateMachine.onOpenEndedInputChanged(intent.node, intent.newInput)
            }

            QuestionnaireIntent.ContinueClicked -> {
                if (!modelState.isFormValid) {
                    val error = stateMachine.findInvalidOpenEndedRegexNodeError()?.let {
                        QuestionnaireError.InvalidOpenEndedRegexMatch(it)
                    } ?: modelState.error
                    updateState { copy(invalidNodes = stateMachine.invalidNodes, error = error) }
                    return
                }

                updateState { copy(isUploadingNodes = true, invalidNodes = emptyList()) }
                val nodes = stateMachine.getRoot().toDomain()
                val filledQuestionnaire = originalQuestionnaire.copy(nodes = nodes)
                when (val result = dataRemediationService.submitQuestionnaire(filledQuestionnaire)) {
                    is Outcome.Success -> {
                        analytics.logEvent(KycQuestionnaireSubmitted)
                        navigate(Navigation.FinishSuccessfully)
                    }

                    is Outcome.Failure -> updateState {
                        val invalidNodeId = (result.failure as? SubmitQuestionnaireError.InvalidNode)?.nodeId
                        val invalidNode = modelState.nodes.find { it.id == invalidNodeId }
                        val error = if (invalidNode != null) {
                            QuestionnaireError.InvalidNode(invalidNode)
                        } else {
                            // Shouldn't occurr as the backend should always send an existing id in the current list
                            val errorMessage = when (val result = result.failure) {
                                is SubmitQuestionnaireError.InvalidNode -> null
                                is SubmitQuestionnaireError.RequestFailed -> result.message
                            }
                            QuestionnaireError.Unknown(errorMessage)
                        }
                        copy(
                            isUploadingNodes = false,
                            error = error,
                            invalidNodes = listOfNotNull(invalidNodeId),
                            isFormValid = invalidNodeId == null
                        )
                    }
                }
            }

            QuestionnaireIntent.ErrorHandled -> updateState { copy(error = null) }
        }.exhaustive
    }

    private fun QuestionnaireStateMachine.findInvalidOpenEndedRegexNodeError(): FlatNode.OpenEnded? {
        if (this.invalidNodes.isEmpty()) return null
        return this.state.filterIsInstance<FlatNode.OpenEnded>()
            .firstOrNull { invalidNodes.contains(it.id) && it.regex != null && !it.regex.matches(it.input) }
    }
}
