package piuk.blockchain.android.ui.kyc.questionnaire

import android.os.Looper
import android.os.Parcelable
import com.blockchain.nabu.models.responses.nabu.NodeId
import kotlinx.parcelize.Parcelize

class KycQuestionnaireStateMachine {

    private val presentable = mutableListOf<FlatNode>()

    val state: List<FlatNode>
        get() {
            assertMainThread()
            return presentable.toList()
        }

    /**
     * After calling this, invalidNodes can be accessed to check which nodes have failed validation
     */
    fun isValid(): Boolean {
        assertMainThread()
        invalidNodeIds.clear()
        return _root.isValid()
    }

    /**
     * isValid() should be called before calling this
     */
    val invalidNodes: List<NodeId>
        get() = invalidNodeIds.toList()

    lateinit var onStateChanged: (List<FlatNode>) -> Unit

    private lateinit var _root: TreeNode.Root
    fun setRoot(root: TreeNode.Root) {
        _root = root
        render(root)
        onStateChanged(state)
    }
    fun getRoot(): TreeNode.Root = _root

    fun onDropdownChoiceChanged(node: FlatNode.Dropdown, newChoice: FlatNode.Selection) {
        assertMainThread()

        val parentTreeNode = _root.findNode { it.id == node.id } as TreeNode.SingleSelection

        parentTreeNode.children
            .filterIsInstance<TreeNode.Selection>()
            .forEach { it.isChecked = it.id == newChoice.id }
        render(parentTreeNode, node.depth)
        onStateChanged(state)
    }

    fun onSelectionClicked(node: FlatNode.Selection) {
        assertMainThread()

        val parentTreeNode = _root.findNode { it.children.any { it.id == node.id } }
        val clickedTreeNode = parentTreeNode?.findNode { it.id == node.id } as TreeNode.Selection

        if (parentTreeNode is TreeNode.SingleSelection && parentTreeNode.isDropdown) {
            throw UnsupportedOperationException("Use `onDropdownChoiceChanged` instead")
        }

        if (parentTreeNode is TreeNode.SingleSelection) {
            parentTreeNode.children.forEach {
                if (it is TreeNode.Selection && it.isChecked) {
                    it.isChecked = false
                    render(it, node.depth)
                }
            }
            clickedTreeNode.isChecked = true
            render(clickedTreeNode, node.depth)
        } else {
            clickedTreeNode.isChecked = !clickedTreeNode.isChecked
            render(clickedTreeNode, node.depth)
        }
        onStateChanged(state)
    }

    fun onOpenEndedInputChanged(node: FlatNode.OpenEnded, newInput: String) {
        assertMainThread()

        val treeNode = _root.findNode { it.id == node.id } as TreeNode.OpenEnded
        treeNode.input = newInput

        render(treeNode, node.depth)
        onStateChanged(state)
    }

    private fun render(node: TreeNode, depth: Int) {
        val indexOfFirst = presentable.indexOfFirst { it.id == node.id }

        val parentTreeNode = _root.findNode { it.children.any { it.id == node.id } }
        val newNodes = node.flatten(depth, parentTreeNode)
        if (node.children.isEmpty()) {
            // parse only the node and replace it in presentables
            presentable.set(indexOfFirst, newNodes.first())
        } else {
            var endOfCurrentChildrenIndex = 0
            var hasReachedEndOfChildren = false
            // we create a subset of the list starting from the newNode index forward
            val searchableSublist = presentable.subList(indexOfFirst, presentable.size)
            searchableSublist.forEachIndexed { index, node ->
                if (hasReachedEndOfChildren) return@forEachIndexed
                // ignore the first node, which is the one that we already know the index of
                if (index == 0) return@forEachIndexed
                // we use the depth to find the changed node children
                if (node.depth > depth) endOfCurrentChildrenIndex = index
                else hasReachedEndOfChildren = true
            }
            val numberOfNodesToRemove = (indexOfFirst..(endOfCurrentChildrenIndex + indexOfFirst)).count()

            repeat(numberOfNodesToRemove) {
                presentable.removeAt(indexOfFirst)
            }
            presentable.addAll(indexOfFirst, newNodes)
        }
    }

    private fun render(root: TreeNode.Root) {
        presentable.clear()
        presentable += root.flatten(0, null)
    }

    private fun assertMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "This method must be called from the MainThread, this class is not threadsafe"
        }
    }

    private val invalidNodeIds = mutableListOf<NodeId>()
    private fun TreeNode.isValid(): Boolean {
        val childrenToValidate = mutableListOf<TreeNode>()
        val isValid = when (this) {
            is TreeNode.Root -> {
                childrenToValidate += children
                true
            }
            is TreeNode.SingleSelection -> {
                val checkedChildren = children.filter { it is TreeNode.Selection && it.isChecked }
                if (checkedChildren.size == 1) {
                    childrenToValidate += checkedChildren
                    true
                } else {
                    false
                }
            }
            is TreeNode.MultipleSelection -> {
                val checkedChildren = children.filter { it is TreeNode.Selection && it.isChecked }

                if (checkedChildren.isNotEmpty()) {
                    childrenToValidate += checkedChildren
                    true
                } else {
                    false
                }
            }
            is TreeNode.OpenEnded -> {
                val isValid = input.isNotEmpty()
                if (isValid) {
                    childrenToValidate += children
                }
                isValid
            }
            is TreeNode.Selection -> {
                childrenToValidate += children
                true
            }
        }

        if (!isValid) {
            invalidNodeIds += this.id
            return false
        }
        childrenToValidate.forEach {
            if (!it.isValid()) return false
        }

        return true
    }

    private fun TreeNode.findNode(predicate: (TreeNode) -> Boolean): TreeNode? {
        if (predicate(this)) return this
        children.forEach {
            val node = it.findNode(predicate)
            if (node != null) return node
        }
        return null
    }

    private fun TreeNode.flatten(depth: Int, parent: TreeNode?): List<FlatNode> {
        val presentable = mutableListOf<FlatNode>()
        if (this !is TreeNode.Root) presentable += this.toViewItem(depth, parent!!)

        // Don't draw the children of Selection unchecked nodes
        if (this is TreeNode.Selection && !this.isChecked) return presentable
        // Don't draw the children of OpenEnded empty nodes
        if (this is TreeNode.OpenEnded && this.input.isBlank()) return presentable

        // Don't draw dropdown children, as this is handled by the view
        val childrenToFlatten = if (this is TreeNode.SingleSelection && this.isDropdown) {
            val checkedNode = this.children.firstOrNull { it is TreeNode.Selection && it.isChecked }
            // If there's a checked node we want to render it's children
            checkedNode?.children ?: emptyList()
        } else {
            this.children
        }

        childrenToFlatten.forEach {
            val children = it.flatten(depth + 1, this)
            presentable += children
        }

        return presentable
    }

    private fun TreeNode.toViewItem(depth: Int, parent: TreeNode): FlatNode = when (this) {
        is TreeNode.Root -> throw IllegalStateException()
        is TreeNode.SingleSelection -> if (isDropdown) {
            val children = children
                .filterIsInstance<TreeNode.Selection>()
                .map { it.toViewItem(depth + 1, this) }
                .filterIsInstance<FlatNode.Selection>()
            FlatNode.Dropdown(id, text, depth, instructions, children, children.firstOrNull { it.isChecked })
        } else {
            FlatNode.SingleSelection(id, text, depth, instructions)
        }
        is TreeNode.MultipleSelection -> FlatNode.MultipleSelection(id, text, depth, instructions)
        is TreeNode.OpenEnded -> FlatNode.OpenEnded(id, text, depth, input, hint)
        is TreeNode.Selection -> FlatNode.Selection(id, text, depth, isChecked, parent is TreeNode.SingleSelection)
    }
}

sealed class FlatNode(
    open val id: NodeId,
    open val text: String,
    open val depth: Int
) {
    data class SingleSelection(
        override val id: NodeId,
        override val text: String,
        override val depth: Int,
        val instructions: String
    ) : FlatNode(id, text, depth)

    data class Dropdown(
        override val id: NodeId,
        override val text: String,
        override val depth: Int,
        val instructions: String,
        val choices: List<Selection>,
        val selectedChoice: Selection?
    ) : FlatNode(id, text, depth)

    data class MultipleSelection(
        override val id: NodeId,
        override val text: String,
        override val depth: Int,
        val instructions: String
    ) : FlatNode(id, text, depth)

    data class OpenEnded(
        override val id: NodeId,
        override val text: String,
        override val depth: Int,
        val input: String,
        val hint: String
    ) : FlatNode(id, text, depth)

    data class Selection(
        override val id: NodeId,
        override val text: String,
        override val depth: Int,
        val isChecked: Boolean,
        val isParentSingleSelection: Boolean
    ) : FlatNode(id, text, depth)
}

sealed class TreeNode(
    open val id: NodeId,
    open val text: String,
    open val children: List<TreeNode>
) : Parcelable {
    @Parcelize
    data class Root(
        override val children: List<TreeNode>
    ) : TreeNode("ROOT", "ROOT", children)

    @Parcelize
    data class SingleSelection(
        override val id: NodeId,
        override val text: String,
        override val children: List<TreeNode>,
        val instructions: String,
        val isDropdown: Boolean
    ) : TreeNode(id, text, children)

    @Parcelize
    data class MultipleSelection(
        override val id: NodeId,
        override val text: String,
        override val children: List<TreeNode>,
        val instructions: String
    ) : TreeNode(id, text, children)

    @Parcelize
    data class OpenEnded(
        override val id: NodeId,
        override val text: String,
        override val children: List<TreeNode>,
        var input: String,
        val hint: String
    ) : TreeNode(id, text, children)

    @Parcelize
    data class Selection(
        override val id: NodeId,
        override val text: String,
        override val children: List<TreeNode>,
        var isChecked: Boolean
    ) : TreeNode(id, text, children)
}
