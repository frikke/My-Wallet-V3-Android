package piuk.blockchain.android.ui.kyc.additional_info

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.nabu.datamanagers.kyc.UpdateKycAdditionalInfoError
import com.blockchain.nabu.models.responses.nabu.NodeId

data class KycAdditionalInfoState(
    val nodes: List<FlatNode> = emptyList(),
    val isContinueEnabled: Boolean = true,
    val isUploadingNodes: Boolean = false,
    val invalidNodesShown: List<NodeId> = emptyList(),
    val error: UpdateKycAdditionalInfoError? = null
) : ViewState
