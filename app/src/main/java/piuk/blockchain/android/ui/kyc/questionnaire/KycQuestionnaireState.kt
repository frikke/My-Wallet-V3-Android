package piuk.blockchain.android.ui.kyc.questionnaire

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.nabu.datamanagers.kyc.SubmitQuestionnaireError
import com.blockchain.nabu.models.responses.nabu.NodeId

data class KycQuestionnaireState(
    val nodes: List<FlatNode> = emptyList(),
    val isContinueEnabled: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodesShown: List<NodeId> = emptyList(),
    val error: SubmitQuestionnaireError? = null
) : ViewState
