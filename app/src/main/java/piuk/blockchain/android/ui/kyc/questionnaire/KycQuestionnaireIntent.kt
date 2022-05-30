package piuk.blockchain.android.ui.kyc.questionnaire

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class KycQuestionnaireIntent : Intent<KycQuestionnaireModelState> {

    data class DropdownChoiceChanged(
        val node: FlatNode.Dropdown,
        val newChoice: FlatNode.Selection
    ) : KycQuestionnaireIntent()

    data class SelectionClicked(val node: FlatNode.Selection) : KycQuestionnaireIntent()

    data class OpenEndedInputChanged(
        val node: FlatNode.OpenEnded,
        val newInput: String
    ) : KycQuestionnaireIntent()

    object ContinueClicked : KycQuestionnaireIntent()

    object ErrorHandled : KycQuestionnaireIntent()
}
