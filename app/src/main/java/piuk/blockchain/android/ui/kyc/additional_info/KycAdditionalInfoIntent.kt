package piuk.blockchain.android.ui.kyc.additional_info

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class KycAdditionalInfoIntent : Intent<KycAdditionalInfoModelState> {

    data class DropdownChoiceChanged(
        val node: FlatNode.Dropdown,
        val newChoice: FlatNode.Selection
    ) : KycAdditionalInfoIntent()

    data class SelectionClicked(val node: FlatNode.Selection) : KycAdditionalInfoIntent()

    data class OpenEndedInputChanged(
        val node: FlatNode.OpenEnded,
        val newInput: String
    ) : KycAdditionalInfoIntent()

    object ContinueClicked : KycAdditionalInfoIntent()

    object ErrorHandled : KycAdditionalInfoIntent()
}
