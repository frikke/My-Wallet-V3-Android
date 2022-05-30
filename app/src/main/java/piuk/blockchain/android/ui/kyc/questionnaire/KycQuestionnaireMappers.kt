package piuk.blockchain.android.ui.kyc.questionnaire

import com.blockchain.nabu.models.responses.nabu.KycQuestionnaireNode

fun TreeNode.Root.toDomain(): List<KycQuestionnaireNode> =
    children.map { it.toDomain() }
fun TreeNode.toDomain(): KycQuestionnaireNode {
    val children = children.map { it.toDomain() }
    return when (this) {
        is TreeNode.Root -> throw UnsupportedOperationException()
        is TreeNode.SingleSelection ->
            KycQuestionnaireNode.SingleSelection(id, text, children, instructions, isDropdown)
        is TreeNode.MultipleSelection -> KycQuestionnaireNode.MultipleSelection(id, text, children, instructions)
        is TreeNode.OpenEnded -> KycQuestionnaireNode.OpenEnded(id, text, children, input, hint)
        is TreeNode.Selection -> KycQuestionnaireNode.Selection(id, text, children, isChecked)
    }
}

fun List<KycQuestionnaireNode>.toMutableNode(): TreeNode.Root =
    TreeNode.Root(this.map { it.toMutableNode() })
fun KycQuestionnaireNode.toMutableNode(): TreeNode = when (this) {
    is KycQuestionnaireNode.SingleSelection ->
        TreeNode.SingleSelection(id, text, children.map { it.toMutableNode() }, instructions, isDropdown)
    is KycQuestionnaireNode.MultipleSelection ->
        TreeNode.MultipleSelection(id, text, children.map { it.toMutableNode() }, instructions)
    is KycQuestionnaireNode.OpenEnded ->
        TreeNode.OpenEnded(id, text, children.map { it.toMutableNode() }, input, hint)
    is KycQuestionnaireNode.Selection ->
        TreeNode.Selection(id, text, children.map { it.toMutableNode() }, isChecked)
}
