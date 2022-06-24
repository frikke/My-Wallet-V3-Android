package piuk.blockchain.android.ui.dataremediation

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class QuestionnaireIntent : Intent<QuestionnaireModelState> {

    data class DropdownChoiceChanged(
        val node: FlatNode.Dropdown,
        val newChoice: FlatNode.Selection
    ) : QuestionnaireIntent()

    data class SelectionClicked(val node: FlatNode.Selection) : QuestionnaireIntent()

    data class OpenEndedInputChanged(
        val node: FlatNode.OpenEnded,
        val newInput: String
    ) : QuestionnaireIntent()

    object ContinueClicked : QuestionnaireIntent()

    object ErrorHandled : QuestionnaireIntent()
}
