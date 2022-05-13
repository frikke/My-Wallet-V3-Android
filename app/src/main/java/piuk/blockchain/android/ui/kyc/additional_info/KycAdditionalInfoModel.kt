package piuk.blockchain.android.ui.kyc.additional_info

import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.datamanagers.kyc.KycDataManager
import com.blockchain.nabu.datamanagers.kyc.UpdateKycAdditionalInfoError
import com.blockchain.nabu.models.responses.nabu.NodeId
import com.blockchain.outcome.Outcome
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.campaign.CampaignType

data class KycAdditionalInfoModelState(
    val nodes: List<FlatNode> = emptyList(),
    val isFormValid: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodes: List<NodeId> = emptyList(),
    val campaignType: CampaignType = CampaignType.None,
    val error: UpdateKycAdditionalInfoError? = null
) : ModelState

sealed class Navigation : NavigationEvent {
    object LaunchVeriff : Navigation()
}

@Parcelize
data class Args(
    val root: TreeNode.Root,
    val campaignType: CampaignType
) : ModelConfigArgs.ParcelableArgs

class KycAdditionalInfoModel(
    private val kycDataManager: KycDataManager,
    private val stateMachine: StateMachine,
    private val analytics: Analytics
) : MviViewModel<
    KycAdditionalInfoIntent,
    KycAdditionalInfoState,
    KycAdditionalInfoModelState,
    Navigation,
    Args
    >(KycAdditionalInfoModelState()) {

    override fun viewCreated(args: Args) {
        analytics.logEvent(KycAdditionalInfoViewed)
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
                        analytics.logEvent(KycAdditionalInfoSubmitted)
                        navigate(Navigation.LaunchVeriff)
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
}
