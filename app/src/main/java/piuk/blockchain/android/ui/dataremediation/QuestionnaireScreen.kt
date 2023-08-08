package piuk.blockchain.android.ui.dataremediation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.Checkbox
import com.blockchain.componentlib.control.CheckboxState
import com.blockchain.componentlib.control.Radio
import com.blockchain.componentlib.control.RadioButtonState
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.domain.dataremediation.model.NodeId
import com.blockchain.domain.dataremediation.model.QuestionnaireHeader
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import piuk.blockchain.android.R

@Composable
fun QuestionnaireScreen(
    showNavigationBar: Boolean,
    isSkipVisible: Boolean,
    header: QuestionnaireHeader?,
    state: QuestionnaireState,
    onDropdownOpenPickerClicked: (node: FlatNode.Dropdown) -> Unit,
    onSelectionClicked: (node: FlatNode.Selection) -> Unit,
    onOpenEndedInputChanged: (node: FlatNode.OpenEnded, newInput: String) -> Unit,
    onContinueClicked: () -> Unit,
    onSkipClicked: () -> Unit,
    onBackClicked: () -> Unit
) {
    Column(
        Modifier
            .background(AppColors.background)
            .fillMaxWidth()
    ) {
        val listState = rememberLazyListState()
        val currentIds = state.nodes.map { it.id }
        val previousIdsState = remember { mutableStateOf(emptyList<NodeId>()) }

        // Checking Selection's will display it's children, if these newly displayed children are offscreen scroll to them
        LaunchedEffect(currentIds) {
            val previousIds = previousIdsState.value
            previousIdsState.value = currentIds

            if (previousIds.isEmpty() || previousIds == currentIds) return@LaunchedEffect
            val newlyAddedIds = (currentIds - previousIds)
            if (newlyAddedIds.isEmpty()) return@LaunchedEffect
            val firstNewlyAddedId = newlyAddedIds.first()
            val isFirstNewlyAddedItemFullyVisible = listState.layoutInfo.visibleItemsInfo
                .find { it.key == firstNewlyAddedId }
                ?.takeIf { listState.layoutInfo.viewportEndOffset > it.offset + it.size } != null

            if (!isFirstNewlyAddedItemFullyVisible) {
                // Because this will scroll in a way that the scrolledToItem will be at the top, and we don't want that because
                // the user would lose context on where he is, because we wouldn't even see the question he clicked in the first place,
                // we scroll 3 items before instead of directly to the first new item
                val indexToScrollTo = (currentIds.indexOf(firstNewlyAddedId) - 3).coerceAtLeast(0)
                listState.animateScrollToItem(indexToScrollTo)
            }
        }

        // When errors are shown we scroll to the first one if it's not fully visible
        LaunchedEffect(state.invalidNodesShown) {
            val firstInvalidNode = state.invalidNodesShown.firstOrNull() ?: return@LaunchedEffect

            val isFullyVisible = listState.layoutInfo.visibleItemsInfo
                .find { it.key == firstInvalidNode }
                ?.takeIf { listState.layoutInfo.viewportEndOffset > it.offset + it.size } != null

            if (!isFullyVisible) {
                val indexToScrollTo = (currentIds.indexOf(firstInvalidNode) - 3).coerceAtLeast(0)
                listState.animateScrollToItem(indexToScrollTo)
            }
        }

        if (showNavigationBar) {
            NavigationBar(
                title = stringResource(com.blockchain.stringResources.R.string.kyc_additional_info_toolbar),
                onBackButtonClick = onBackClicked,
                navigationBarButtons = if (isSkipVisible) {
                    listOf(
                        NavigationBarButton.Text(
                            stringResource(com.blockchain.stringResources.R.string.common_skip),
                            AppColors.primary,
                            onSkipClicked
                        )
                    )
                } else {
                    emptyList()
                }
            )
        }

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                bottom = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState
        ) {
            if (header != null) {
                item {
                    Header(header)
                }
            }

            var currentNodeIndex = 0
            while (currentNodeIndex != state.nodes.size - 1) {
                val currentNode = state.nodes[currentNodeIndex]
                val nextNode = state.nodes[currentNodeIndex + 1]

                if (currentNode is FlatNode.MultipleSelection &&
                    nextNode is FlatNode.Selection && !nextNode.isParentSingleSelection
                ) {
                    // show multiple selection question header
                    item {
                        NodeRow(
                            currentNode,
                            state.invalidNodesShown.contains(currentNode.id),
                            onDropdownOpenPickerClicked,
                            onSelectionClicked,
                            onOpenEndedInputChanged
                        )
                    }

                    // retrieve the possible options for this question
                    val multipleSelectionChildNodes = mutableListOf<FlatNode.Selection>()
                    var currentMultipleSelectionChildNodeIndex = currentNodeIndex + 1
                    var hasNext = true
                    while (hasNext) {
                        val multiSelectionChildNode = state.nodes[currentMultipleSelectionChildNodeIndex]
                        if (multiSelectionChildNode is FlatNode.Selection) {
                            multipleSelectionChildNodes.add(multiSelectionChildNode)
                            currentMultipleSelectionChildNodeIndex++
                        } else {
                            currentNodeIndex += currentMultipleSelectionChildNodeIndex
                            hasNext = false
                        }
                    }

                    // display the options
                    item {
                        FlowSelectionNodeRow(multipleSelectionChildNodes.toImmutableList(), onSelectionClicked)
                    }
                } else {
                    item {
                        NodeRow(
                            currentNode,
                            state.invalidNodesShown.contains(currentNode.id),
                            onDropdownOpenPickerClicked,
                            onSelectionClicked,
                            onOpenEndedInputChanged
                        )
                    }
                }

                currentNodeIndex++
            }
        }

        Box(
            Modifier.padding(
                start = dimensionResource(id = com.blockchain.componentlib.R.dimen.standard_spacing),
                end = dimensionResource(id = com.blockchain.componentlib.R.dimen.standard_spacing),
                bottom = dimensionResource(id = com.blockchain.componentlib.R.dimen.standard_spacing)
            )
        ) {
            PrimaryButton(
                text = stringResource(com.blockchain.stringResources.R.string.common_continue),
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinueClicked,
                state = when {
                    state.isUploadingNodes -> ButtonState.Loading
                    state.isContinueEnabled -> ButtonState.Enabled
                    else -> ButtonState.Disabled
                }
            )
            // Disabled buttons don't handle clicks, we still want the user to
            // be able to click this area so that we show the invalid nodes
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onContinueClicked() }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FlowSelectionNodeRow(
    multipleSelectionChildNodes: ImmutableList<FlatNode.Selection>,
    onSelectionClicked: (node: FlatNode.Selection) -> Unit
) {
    FlowRow(modifier = Modifier.padding(horizontal = AppTheme.dimensions.standardSpacing)) {
        multipleSelectionChildNodes.forEach { multiSelectionNode ->

            val isChecked = multiSelectionNode.isChecked

            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(AppTheme.dimensions.smallestSpacing)
                    .clip(AppTheme.shapes.small)
                    .background(
                        color = if (isChecked) AppTheme.colors.primary
                        else AppTheme.colors.backgroundSecondary
                    )
                    .clickable {
                        onSelectionClicked(multiSelectionNode)
                    }
            ) {
                Text(
                    text = multiSelectionNode.text,
                    style = AppTheme.typography.paragraph2,
                    color = if (isChecked) AppTheme.colors.backgroundSecondary else AppTheme.colors.primary,
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.tinySpacing, horizontal = AppTheme.dimensions.smallSpacing
                    )
                )
            }
        }
    }
}

@Composable
private fun NodeRow(
    node: FlatNode,
    isInvalid: Boolean,
    onDropdownOpenPickerClicked: (node: FlatNode.Dropdown) -> Unit,
    onSelectionClicked: (node: FlatNode.Selection) -> Unit,
    onOpenEndedInputChanged: (node: FlatNode.OpenEnded, newInput: String) -> Unit
) {
    val topPadding = when (node) {
        is FlatNode.SingleSelection,
        is FlatNode.Dropdown,
        is FlatNode.MultipleSelection
        -> dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)

        is FlatNode.OpenEnded,
        is FlatNode.Selection
        -> 0.dp
    }

    val commonModifier = Modifier
        .let { if (isInvalid) it.background(Color.Red.copy(alpha = .2f)) else it }
        .padding(
            top = topPadding,
            start = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
            end = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
        )

    when (node) {
        is FlatNode.SingleSelection -> SingleSelectionRow(commonModifier, node, isInvalid)
        is FlatNode.Dropdown -> DropdownRow(
            commonModifier,
            node,
            isInvalid,
            onSelectionClicked,
            onDropdownOpenPickerClicked
        )

        is FlatNode.MultipleSelection -> MultipleSelectionRow(commonModifier, node, isInvalid)
        is FlatNode.OpenEnded -> OpenEndedRow(commonModifier, node, isInvalid, onOpenEndedInputChanged)
        is FlatNode.Selection -> SelectionRow(commonModifier, node, onSelectionClicked)
    }
}

@Composable
private fun SingleSelectionRow(
    modifier: Modifier,
    node: FlatNode.SingleSelection,
    isInvalid: Boolean
) {
    Column(modifier) {
        SimpleText(
            text = node.text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        SimpleText(
            text = node.instructions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
    }
}

@Composable
private fun DropdownRow(
    modifier: Modifier,
    node: FlatNode.Dropdown,
    isInvalid: Boolean,
    onSelectionClicked: (node: FlatNode.Selection) -> Unit,
    onDropdownOpenPickerClicked: (node: FlatNode.Dropdown) -> Unit
) {
    val showChoicesAsMenu = node.choices.size <= 10
    Column(modifier) {
        SimpleText(
            text = node.text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        SimpleText(
            text = node.instructions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .padding(bottom = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        var isExpanded by remember { mutableStateOf(false) }

        Box {
            val value = node.selectedChoices.map { it.text }.joinToString()
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                isError = isInvalid,
                trailingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_chevron_down),
                        null,
                        Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)),
                        AppColors.body
                    )
                },
                textStyle = AppTheme.typography.body1,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = AppColors.title,
                    focusedBorderColor = AppColors.medium,
                    unfocusedBorderColor = AppColors.medium
                )

            )
            // TextFields are not clickable so this workaround is necessary
            Box(
                Modifier
                    .matchParentSize()
                    .clickable {
                        if (showChoicesAsMenu) {
                            isExpanded = true
                        } else {
                            onDropdownOpenPickerClicked(node)
                        }
                    }
            )
        }

        if (showChoicesAsMenu) {
            DropdownMenu(
                modifier = Modifier.background(AppColors.backgroundSecondary),
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                node.choices.forEach { choice ->
                    DropdownMenuItem(onClick = {
                        if (!node.isMultiSelection) isExpanded = false
                        onSelectionClicked(choice)
                    }) {
                        Text(choice.text, color = AppColors.title)

                        if (choice.id == node.selectedChoices.firstOrNull()?.id) {
                            Image(
                                modifier = Modifier
                                    .padding(
                                        start = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)
                                    ),
                                imageResource = Icons.Filled.Check
                                    .withTint(AppColors.primary)
                                    .withSize(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultipleSelectionRow(
    modifier: Modifier,
    node: FlatNode.MultipleSelection,
    isInvalid: Boolean
) {
    Column(modifier) {
        SimpleText(
            text = node.text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        SimpleText(
            text = node.instructions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
    }
}

@Composable
private fun OpenEndedRow(
    modifier: Modifier,
    node: FlatNode.OpenEnded,
    isInvalid: Boolean,
    onOpenEndedInputChanged: (node: FlatNode.OpenEnded, newInput: String) -> Unit
) {
    var input: String by remember { mutableStateOf(node.input) }

    Column(
        modifier.fillMaxWidth()
    ) {
        if (node.text.isNotEmpty()) {
            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                text = node.text,
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = input,
            onValueChange = {
                input = it
                onOpenEndedInputChanged(node, it)
            },
            placeholder = if (node.hint.isNotEmpty()) {
                { Text(node.hint) }
            } else {
                null
            },
            singleLine = true,
            isError = isInvalid,
            textStyle = AppTheme.typography.body1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = AppColors.title,
                focusedBorderColor = AppColors.medium,
                unfocusedBorderColor = AppColors.medium,
                cursorColor = AppColors.primary
            )
        )
    }
}

@Composable
internal fun SelectionRow(
    modifier: Modifier,
    node: FlatNode.Selection,
    onSelectionClicked: (node: FlatNode.Selection) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectionClicked(node) }
            .border(1.dp, AppColors.medium, RoundedCornerShape(8.dp))
            .padding(
                start = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing),
                end = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
            )
    ) {

        if (node.isParentSingleSelection) {
            Radio(state = if (node.isChecked) RadioButtonState.Selected else RadioButtonState.Unselected)
        } else {
            Checkbox(
                modifier = Modifier.padding(dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)),
                state = if (node.isChecked) CheckboxState.Checked else CheckboxState.Unchecked
            )
        }

        SimpleText(
            text = node.text,
            modifier = Modifier
                .weight(1f)
                .padding(
                    end = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing),
                    top = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing),
                    bottom = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)
                ),
            style = ComposeTypographies.Body2,
            color = if (node.isChecked) ComposeColors.Title else ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
    }
}

@Composable
private fun Header(header: QuestionnaireHeader) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(Color.White, CircleShape)
                .size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(58.dp),
                imageResource = Icons.Filled.User
            )
        }
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                    start = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                    end = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
                ),
            text = header.title,
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start
        )
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing),
                    bottom = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                    start = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                    end = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
                ),
            text = header.description,
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
    }
}

private val previewNodes = listOf(
    FlatNode.MultipleSelection(
        id = "q1",
        text = "Nature & Purpose of Business Relationship",
        instructions = "(Select all that apply)",
        depth = 1
    ),
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
        isChecked = false,
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
        isChecked = false,
        isParentSingleSelection = false
    ),
    FlatNode.Selection(id = "q1-a5", text = "Business", depth = 2, isChecked = false, isParentSingleSelection = false),
    FlatNode.Dropdown(
        id = "q2",
        text = "Source of funds",
        instructions = "(Select only one)",
        depth = 1,
        isMultiSelection = false,
        choices = listOf(
            FlatNode.Selection(
                id = "q2-a1",
                text = "Salary",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = true
            ),
            FlatNode.Selection(
                id = "q2-a2",
                text = "Crypto Trading",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = true
            ),
            FlatNode.Selection(
                id = "q2-a3",
                text = "Crypto Mining",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = true
            ),
            FlatNode.Selection(
                id = "q2-a4",
                text = "Investment Income",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = true
            ),
            FlatNode.Selection(
                id = "q2-a5",
                text = "Real Estate",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = true
            ),
            FlatNode.Selection(
                id = "q2-a6",
                text = "Inheritance",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = true
            ),
            FlatNode.Selection(
                id = "q2-a7",
                text = "Other",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = true
            )
        ),
        selectedChoices = emptyList()
    ),
    FlatNode.Dropdown(
        id = "q3",
        text = "Are you acting on your own behalf?",
        instructions = "(Select only one)",
        depth = 1,
        isMultiSelection = false,
        choices = listOf(
            FlatNode.Selection(
                id = "q3-a1",
                text = "Yes",
                depth = 2,
                isChecked = false,
                isParentSingleSelection = true
            ),
            FlatNode.Selection(id = "q3-a2", text = "No", depth = 2, isChecked = false, isParentSingleSelection = true)
        ),
        selectedChoices = emptyList()
    ),
    FlatNode.SingleSelection(
        id = "q4",
        text = "Are you a Politically Exposed Person (PEP)",
        instructions = "(Select only one)",
        depth = 1
    ),
    FlatNode.Selection(id = "q4-a1", text = "No", depth = 2, isChecked = false, isParentSingleSelection = true),
    FlatNode.Selection(id = "q4-a2", text = "Yes, I am", depth = 2, isChecked = false, isParentSingleSelection = true),
    FlatNode.Selection(
        id = "q4-a3",
        text = "Yes, My Family Member Or Close Associate Is",
        depth = 2,
        isChecked = false,
        isParentSingleSelection = true
    )
)

@Preview
@Composable
private fun ScreenPreview() {
    val state = QuestionnaireState(
        nodes = previewNodes,
        isContinueEnabled = false,
        isUploadingNodes = false,
        invalidNodesShown = emptyList(),
        error = null
    )

    QuestionnaireScreen(
        showNavigationBar = false,
        isSkipVisible = false,
        header = QuestionnaireHeader(
            title = "Additional Information Needed",
            description = "To comply with your country’s regulation, we need a few more pieces of " +
                "information before you’re all set up to start trading crypto."
        ),
        state = state,
        onDropdownOpenPickerClicked = {},
        onSelectionClicked = {},
        onOpenEndedInputChanged = { _, _ -> },
        onContinueClicked = {},
        onSkipClicked = {},
        onBackClicked = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScreenPreviewDark() {
    ScreenPreview()
}
