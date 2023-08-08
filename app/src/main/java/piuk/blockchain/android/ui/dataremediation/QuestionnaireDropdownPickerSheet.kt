package piuk.blockchain.android.ui.dataremediation

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import kotlinx.coroutines.flow.MutableStateFlow

class QuestionnaireDropdownPickerSheet : ComposeModalBottomDialog() {

    interface Host : HostedBottomSheet.Host {
        fun selectionChanged(node: FlatNode.Dropdown, newSelectedChoices: List<FlatNode.Selection>)
    }

    override val makeSheetNonCollapsible: Boolean = true

    override val host: Host by lazy {
        (parentFragment as? Host) ?: throw IllegalStateException(
            "Host is not a QuestionnaireFragment.Host"
        )
    }

    private val original: FlatNode.Dropdown by lazy {
        arguments?.getParcelable(ARG_ORIGINAL)!!
    }

    private val node: TreeNode by lazy {
        arguments?.getParcelable(ARG_NODE)!!
    }

    // We don't support nested nodes in here, they'll be presented for SingleSelection on the QuestionnaireSheet instead
    private val shallowRoot: TreeNode.Root by lazy {
        val shallowChildren = node.children.map {
            when (it) {
                is TreeNode.Root -> throw UnsupportedOperationException()
                is TreeNode.MultipleSelection -> it.copy(children = emptyList())
                is TreeNode.OpenEnded -> it.copy(children = emptyList())
                is TreeNode.Selection -> it.copy(children = emptyList())
                is TreeNode.SingleSelection -> it.copy(children = emptyList())
            }
        }
        if (node is TreeNode.SingleSelection) {
            TreeNode.Root(
                listOf((node as TreeNode.SingleSelection).copy(children = shallowChildren, isDropdown = false))
            )
        } else if (node is TreeNode.MultipleSelection) {
            TreeNode.Root(
                listOf((node as TreeNode.MultipleSelection).copy(children = shallowChildren, isDropdown = false))
            )
        } else {
            throw UnsupportedOperationException()
        }
    }

    private val questionnaireState = MutableStateFlow<List<FlatNode.Selection>>(emptyList())
    private val stateMachine = QuestionnaireStateMachine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateMachine.onStateChanged = {
            // We remove the top element Single/MultiSelection because we already render the title and instructions separately
            val choices = it.subList(1, it.size)
            questionnaireState.value = choices.filterIsInstance<FlatNode.Selection>()
        }
        stateMachine.setRoot(shallowRoot)
    }

    @Composable
    override fun Sheet() {
        val nodes by questionnaireState.collectAsStateLifecycleAware()
        Screen(
            title = original.text,
            instructions = original.instructions,
            choices = nodes,
            onSelectionClicked = stateMachine::onSelectionClicked,
            onClosePress = {
                // For ease of use we're also saving the changes when the user clicks the close button, otherwise
                // the user would have to dismiss the keyboard and press the save button
                host.selectionChanged(original, questionnaireState.value.filter { it.isChecked })
                dismiss()
            },
            onSaveClicked = {
                host.selectionChanged(original, questionnaireState.value.filter { it.isChecked })
                dismiss()
            }
        )
    }

    companion object {
        private const val ARG_ORIGINAL = "ARG_ORIGINAL"
        private const val ARG_NODE = "ARG_NODE"

        fun newInstance(
            original: FlatNode.Dropdown,
            node: TreeNode.SingleSelection
        ): QuestionnaireDropdownPickerSheet =
            QuestionnaireDropdownPickerSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ORIGINAL, original)
                    putParcelable(ARG_NODE, node)
                }
            }

        fun newInstance(
            original: FlatNode.Dropdown,
            node: TreeNode.MultipleSelection
        ): QuestionnaireDropdownPickerSheet =
            QuestionnaireDropdownPickerSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ORIGINAL, original)
                    putParcelable(ARG_NODE, node)
                }
            }
    }
}

@Composable
private fun Screen(
    title: String,
    instructions: String,
    choices: List<FlatNode.Selection>,
    onSelectionClicked: (FlatNode.Selection) -> Unit,
    onClosePress: () -> Unit,
    onSaveClicked: () -> Unit
) {
    var stateSearchInput by remember { mutableStateOf("") }

    @Suppress("RememberReturnType")
    val stateVisibleChoices: List<FlatNode.Selection> = remember(choices, stateSearchInput) {
        choices.filter { node ->
            node.text.contains(stateSearchInput, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(AppColors.background, shape = AppTheme.shapes.veryLarge.topOnly())
    ) {
        SheetHeader(
            title = title,
            byline = instructions,
            onClosePress = onClosePress
        )

        CancelableOutlinedSearch(
            modifier = Modifier
                .padding(
                    top = AppTheme.dimensions.standardSpacing,
                    start = AppTheme.dimensions.standardSpacing,
                    end = AppTheme.dimensions.standardSpacing
                ),
            onValueChange = {
                stateSearchInput = it
            },
            placeholder = stringResource(com.blockchain.stringResources.R.string.search)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    top = AppTheme.dimensions.smallSpacing,
                    start = AppTheme.dimensions.standardSpacing,
                    end = AppTheme.dimensions.standardSpacing
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {

                items(
                    items = stateVisibleChoices,
                    key = { it.id },
                    itemContent = { node ->
                        SelectionRow(
                            modifier = Modifier,
                            node = node,
                            onSelectionClicked = onSelectionClicked
                        )
                    }
                )
            }
        )

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = AppTheme.dimensions.standardSpacing),
            text = stringResource(com.blockchain.stringResources.R.string.common_save),
            onClick = onSaveClicked
        )
    }
}

@Preview
@Composable
private fun ScreenPreview() {
    Screen(
        title = "Some title",
        instructions = "Some instructions",
        choices = listOf(
            FlatNode.Selection(
                id = "q1-a1",
                text = "Buy cryptocurrency with cards or bank transfer",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = false
            ),
            FlatNode.Selection(
                id = "q1-a2",
                text = "Swap my cryptocurrencies",
                depth = 2,
                isChecked = true,
                isParentSingleSelection = false
            ),
            FlatNode.Selection(
                id = "q1-a3",
                text = "Send Cryptocurrencies to family or friends",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = false
            ),
            FlatNode.Selection(
                id = "q1-a4",
                text = "Online Purchases",
                depth = 2,
                isChecked = true,
                isParentSingleSelection = false
            )
        ),
        onSelectionClicked = {},
        onClosePress = {},
        onSaveClicked = {}
    )
}
