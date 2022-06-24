package piuk.blockchain.android.ui.dataremediation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.domain.dataremediation.model.NodeId
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError

data class QuestionnaireState(
    val nodes: List<FlatNode> = emptyList(),
    val isContinueEnabled: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodesShown: List<NodeId> = emptyList(),
    val error: SubmitQuestionnaireError? = null
) : ViewState
