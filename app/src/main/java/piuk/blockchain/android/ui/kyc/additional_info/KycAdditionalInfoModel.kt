package piuk.blockchain.android.ui.kyc.additional_info

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.kyc.KycDataManager
import com.blockchain.nabu.datamanagers.kyc.UpdateKycAdditionalInfoError
import com.blockchain.nabu.models.responses.nabu.NodeId
import com.blockchain.network.PollService
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import java.lang.UnsupportedOperationException
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.kyc.address.KycNextStepDecision
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

data class KycAdditionalInfoModelState(
    val nodes: List<FlatNode> = emptyList(),
    val isFormValid: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodes: List<NodeId> = emptyList(),
    val campaignType: CampaignType = CampaignType.None,
    val error: UpdateKycAdditionalInfoError? = null
) : ModelState

sealed class Navigation : NavigationEvent {
    object FinishWithSddComplete : Navigation()
    object FinishWithTier1Complete : Navigation()
    object LaunchVeriff : Navigation()
    object LaunchKycSplash : Navigation()
}

@Parcelize
data class Args(
    val root: TreeNode.Root,
    val campaignType: CampaignType
) : ModelConfigArgs.ParcelableArgs

class KycAdditionalInfoModel(
    private val kycDataManager: KycDataManager,
    private val stateMachine: StateMachine,
    private val custodialWalletManager: CustodialWalletManager,
    private val analytics: Analytics,
    private val kycNextStepDecision: KycNextStepDecision
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
                        when (val result = verifySddAndGetNextStep()) {
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

    private suspend fun verifySddAndGetNextStep(): Outcome<Exception, KycNextStepDecision.NextStep> =
        kycNextStepDecision.nextStep().awaitOutcome()
            .flatMap { checkSddVerificationAndGetNextStep(it) }

    private suspend fun checkSddVerificationAndGetNextStep(
        nextStep: KycNextStepDecision.NextStep
    ): Outcome<Exception, KycNextStepDecision.NextStep> {
        val campaignType = modelState.campaignType

        return custodialWalletManager.isSimplifiedDueDiligenceEligible().awaitOutcome()
            .doOnSuccess { if (it) analytics.logEventOnce(SDDAnalytics.SDD_ELIGIBLE) }
            .flatMap {
                PollService(custodialWalletManager.fetchSimplifiedDueDiligenceUserState()) { sddState ->
                    sddState.stateFinalised
                }.start(timerInSec = 1, retries = 10).map { sddState ->
                    if (sddState.value.isVerified) {
                        if (shouldNotContinueToNextKycTier(nextStep, campaignType)) {
                            KycNextStepDecision.NextStep.SDDComplete
                        } else {
                            nextStep
                        }
                    } else {
                        nextStep
                    }
                }.awaitOutcome()
            }
    }

    private fun shouldNotContinueToNextKycTier(
        nextStep: KycNextStepDecision.NextStep,
        campaignType: CampaignType
    ): Boolean {
        return nextStep < KycNextStepDecision.NextStep.SDDComplete ||
            campaignType == CampaignType.SimpleBuy
    }

    private fun KycNextStepDecision.NextStep.toNavigation(): Navigation = when (this) {
        is KycNextStepDecision.NextStep.MissingAdditionalInfo -> throw UnsupportedOperationException()
        KycNextStepDecision.NextStep.SDDComplete -> Navigation.FinishWithSddComplete
        KycNextStepDecision.NextStep.Tier1Complete -> Navigation.FinishWithTier1Complete
        KycNextStepDecision.NextStep.Tier2Continue -> Navigation.LaunchVeriff
        KycNextStepDecision.NextStep.Tier2ContinueTier1NeedsMoreInfo -> Navigation.LaunchKycSplash
    }
}
