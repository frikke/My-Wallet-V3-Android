package com.blockchain.componentlib.tablerow.custom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.tablerow.FlexibleTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.tag.TagsRow
import com.blockchain.componentlib.theme.AppTheme

@Composable
private fun StyledText(
    text: String,
    style: ViewStyle.TextStyle,
    textAlign: TextAlign
) {
    Text(
        text = text,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = style.style.copy(
            textDecoration = style.textDecoration()
        ),
        color = style.color,
        textAlign = textAlign
    )
}

@Composable
fun CustomTableRow(
    icon: StackedIcon = StackedIcon.None,
    leadingComponents: List<ViewType>,
    trailingComponents: List<ViewType>,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    backgroundShape: Shape = RectangleShape
) {
    FlexibleTableRow(
        paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
        contentStart = {
            CustomStackedIcon(icon = icon)
        },
        content = {
            if (icon !is StackedIcon.None) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            }

            Column {
                leadingComponents.forEachIndexed { index, viewType ->
                    SingleComponent(viewType, isTrailing = false)

                    if (index < leadingComponents.lastIndex) {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                    }
                }
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.verySmallSpacing))

            Column(
                modifier = Modifier.weight(1F),
                horizontalAlignment = Alignment.End
            ) {
                trailingComponents.forEachIndexed { index, viewType ->
                    SingleComponent(viewType = viewType, isTrailing = true)

                    if (index < trailingComponents.lastIndex) {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                    }
                }
            }
        },
        onContentClicked = onClick,
        backgroundColor = backgroundColor,
        backgroundShape = backgroundShape
    )
}

@Composable
private fun SingleComponent(
    viewType: ViewType,
    isTrailing: Boolean
) {
    when (viewType) {
        is ViewType.Text -> {
            StyledText(
                text = viewType.value,
                style = viewType.style,
                textAlign = if (isTrailing) TextAlign.End else TextAlign.Start
            )
        }

        is ViewType.Tag -> {
            TagsRow(
                listOf(
                    TagViewState(
                        value = viewType.value,
                        type = viewType.style
                    )
                )
            )
        }

        ViewType.Unknown -> { // n/a
        }
    }
}

@Preview
@Composable
private fun PreviewCustomTableRow_Summary_SmallTag() {
    CustomTableRow(
        icon = StackedIcon.SmallTag(
            main = ImageResource.Local(R.drawable.ic_close_circle_dark),
            tag = ImageResource.Local(R.drawable.ic_close_circle)
        ),
        leadingComponents = listOf(
            ViewType.Text(
                value = "Sent Ethereum",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "June 14",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            )
        ),
        trailingComponents = listOf(
            ViewType.Text(
                value = "-100.00",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "-21.07674621 UNI",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            )
        ),
        onClick = {}
    )
}

@Preview
@Composable
private fun PreviewCustomTableRow_Summary_StackedIcon() {
    CustomTableRow(
        icon = StackedIcon.OverlappingPair(
            front = ImageResource.Local(R.drawable.ic_close_circle_dark),
            back = ImageResource.Local(R.drawable.ic_close_circle)
        ),
        leadingComponents = listOf(
            ViewType.Text(
                value = "Sent Ethereum",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "June 14",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            )
        ),
        trailingComponents = listOf(
            ViewType.Text(
                value = "-100.00",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "-21.07674621 UNI",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            )
        ),
        onClick = {}
    )
}

@Preview
@Composable
private fun PreviewCustomTableRow_Summary_SingleIcon() {
    CustomTableRow(
        icon = StackedIcon.SingleIcon(
            icon = ImageResource.Local(R.drawable.ic_close_circle_dark)
        ),
        leadingComponents = listOf(
            ViewType.Text(
                value = "Sent Ethereum",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "June 14",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            )
        ),
        trailingComponents = listOf(
            ViewType.Text(
                value = "-100.00",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "-21.07674621 UNI",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            )
        ),
        onClick = {}
    )
}

@Preview
@Composable
private fun PreviewCustomTableRow() {
    CustomTableRow(
        leadingComponents = listOf(
            ViewType.Text(
                value = "Merchant Name",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "June 4",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.warning
                )
            ),
            ViewType.Text(
                value = "June 4 4 4",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            )
        ),
        trailingComponents = listOf(
            ViewType.Text(
                value = "-100.00",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "-21.07674621 UNI",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            ),
            ViewType.Text(
                value = "-21.74621 UNI",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.success
                )
            )
        ),
        onClick = {}
    )
}

@Preview
@Composable
private fun PreviewCustomTableRow_Key_MultiValue() {
    CustomTableRow(
        leadingComponents = listOf(
            ViewType.Text(
                value = "Sale price",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.muted
                )
            )
        ),
        trailingComponents = listOf(
            ViewType.Text(
                value = "-100.00",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "-21.07674621 UNI",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            )
        ),
        onClick = {}
    )
}

@Preview
@Composable
private fun PreviewCustomTableRow_KeyValue() {
    CustomTableRow(
        leadingComponents = listOf(
            ViewType.Text(
                value = "Fees",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            )
        ),
        trailingComponents = listOf(
            ViewType.Text(
                value = "Free",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.success
                )
            )
        ),
        onClick = {}
    )
}

@Preview
@Composable
private fun PreviewCustomTableRow_Tag() {
    CustomTableRow(
        leadingComponents = listOf(
            ViewType.Text(
                value = "Status",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            ),
            ViewType.Text(
                value = "Available in 3-5 days",
                style = ViewStyle.TextStyle(
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.muted
                )
            )
        ),
        trailingComponents = listOf(
            ViewType.Tag(
                value = "Pending",
                style = TagType.InfoAlt()
            )
        ),
        onClick = {}
    )
}
