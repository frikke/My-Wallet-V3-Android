package piuk.blockchain.android.ui.dataremediation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.domain.dataremediation.model.NodeId

data class QuestionnaireState(
    val nodes: List<FlatNode> = emptyList(),
    val isContinueEnabled: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodesShown: List<NodeId> = emptyList(),
    val error: QuestionnaireError? = null,
) : ViewState

sealed class QuestionnaireError {
    data class Unknown(val message: String?) : QuestionnaireError()
    data class InvalidNode(val node: FlatNode) : QuestionnaireError()
    data class InvalidOpenEndedRegexMatch(val node: FlatNode.OpenEnded) : QuestionnaireError()
}
