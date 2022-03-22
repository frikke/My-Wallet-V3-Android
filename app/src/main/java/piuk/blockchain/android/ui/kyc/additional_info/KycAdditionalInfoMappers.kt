package piuk.blockchain.android.ui.kyc.additional_info

import com.blockchain.nabu.models.responses.nabu.KycAdditionalInfoNode

fun TreeNode.Root.toDomain(): List<KycAdditionalInfoNode> =
    children.map { it.toDomain() }
fun TreeNode.toDomain(): KycAdditionalInfoNode {
    val children = children.map { it.toDomain() }
    return when (this) {
        is TreeNode.Root -> throw UnsupportedOperationException()
        is TreeNode.SingleSelection ->
            KycAdditionalInfoNode.SingleSelection(id, text, children, instructions, isDropdown)
        is TreeNode.MultipleSelection -> KycAdditionalInfoNode.MultipleSelection(id, text, children, instructions)
        is TreeNode.OpenEnded -> KycAdditionalInfoNode.OpenEnded(id, text, children, input, hint)
        is TreeNode.Selection -> KycAdditionalInfoNode.Selection(id, text, children, isChecked)
    }
}

fun List<KycAdditionalInfoNode>.toMutableNode(): TreeNode.Root =
    TreeNode.Root(this.map { it.toMutableNode() })
fun KycAdditionalInfoNode.toMutableNode(): TreeNode = when (this) {
    is KycAdditionalInfoNode.SingleSelection ->
        TreeNode.SingleSelection(id, text, children.map { it.toMutableNode() }, instructions, isDropdown)
    is KycAdditionalInfoNode.MultipleSelection ->
        TreeNode.MultipleSelection(id, text, children.map { it.toMutableNode() }, instructions)
    is KycAdditionalInfoNode.OpenEnded ->
        TreeNode.OpenEnded(id, text, children.map { it.toMutableNode() }, input, hint)
    is KycAdditionalInfoNode.Selection ->
        TreeNode.Selection(id, text, children.map { it.toMutableNode() }, isChecked)
}
