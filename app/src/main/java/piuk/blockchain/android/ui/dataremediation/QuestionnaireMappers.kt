package piuk.blockchain.android.ui.dataremediation

import com.blockchain.domain.dataremediation.model.QuestionnaireNode

fun TreeNode.Root.toDomain(): List<QuestionnaireNode> =
    children.map { it.toDomain() }
fun TreeNode.toDomain(): QuestionnaireNode {
    val children = children.map { it.toDomain() }
    return when (this) {
        is TreeNode.Root -> throw UnsupportedOperationException()
        is TreeNode.SingleSelection ->
            QuestionnaireNode.SingleSelection(id, text, children, instructions, isDropdown)
        is TreeNode.MultipleSelection ->
            QuestionnaireNode.MultipleSelection(id, text, children, instructions, isDropdown)
        is TreeNode.OpenEnded -> QuestionnaireNode.OpenEnded(id, text, children, input, hint, regex)
        is TreeNode.Selection -> QuestionnaireNode.Selection(id, text, children, isChecked)
    }
}

fun List<QuestionnaireNode>.toMutableNode(): TreeNode.Root =
    TreeNode.Root(this.map { it.toMutableNode() })
fun QuestionnaireNode.toMutableNode(): TreeNode = when (this) {
    is QuestionnaireNode.SingleSelection ->
        TreeNode.SingleSelection(id, text, children.map { it.toMutableNode() }, instructions, isDropdown)
    is QuestionnaireNode.MultipleSelection ->
        TreeNode.MultipleSelection(id, text, children.map { it.toMutableNode() }, instructions, isDropdown)
    is QuestionnaireNode.OpenEnded ->
        TreeNode.OpenEnded(id, text, children.map { it.toMutableNode() }, input, hint, regex)
    is QuestionnaireNode.Selection ->
        TreeNode.Selection(id, text, children.map { it.toMutableNode() }, isChecked)
}
