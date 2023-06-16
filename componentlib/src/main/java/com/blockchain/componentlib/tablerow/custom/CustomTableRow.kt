package com.blockchain.componentlib.tablerow.custom

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.MaskStateConfig
import com.blockchain.componentlib.basic.MaskableText
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.Email
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.FlexibleTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.tag.TagsRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
private fun StyledText(
    text: String,
    style: ViewStyle.TextStyle,
    textAlign: TextAlign,
    maskState: MaskStateConfig
) {
    MaskableText(
        maskState = maskState,
        text = text,
        style = style.style.copy(
            textDecoration = style.textDecoration()
        ),
        color = style.color,
        textAlign = textAlign
    )
}

@Composable
fun MaskedCustomTableRow(
    modifier: Modifier = Modifier,
    icon: StackedIcon = StackedIcon.None,
    leadingComponents: List<ViewType>,
    trailingComponents: List<ViewType>,
    endIcon: ImageResource = ImageResource.None,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    backgroundShape: Shape = RectangleShape
) {
    CustomTableRow(
        modifier= modifier,
        maskState = MaskStateConfig.Default,
        icon = icon,
        leadingComponents = leadingComponents,
        trailingComponents = trailingComponents,
        endIcon = endIcon,
        onClick = onClick,
        backgroundColor = backgroundColor,
        backgroundShape = backgroundShape
    )
}

@Composable
fun CustomTableRow(
    modifier: Modifier = Modifier,
    icon: StackedIcon = StackedIcon.None,
    leadingComponents: List<ViewType>,
    trailingComponents: List<ViewType>,
    endIcon: ImageResource = ImageResource.None,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    backgroundShape: Shape = RectangleShape
) {
    CustomTableRow(
        modifier = modifier,
        maskState = MaskStateConfig.Override(maskEnabled = false),
        icon = icon,
        leadingComponents = leadingComponents,
        trailingComponents = trailingComponents,
        endIcon = endIcon,
        onClick = onClick,
        backgroundColor = backgroundColor,
        backgroundShape = backgroundShape
    )
}

@Composable
private fun CustomTableRow(
    modifier: Modifier = Modifier,
    maskState: MaskStateConfig,
    icon: StackedIcon = StackedIcon.None,
    leadingComponents: List<ViewType>,
    trailingComponents: List<ViewType>,
    endIcon: ImageResource = ImageResource.None,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    backgroundShape: Shape = RectangleShape
) {
    FlexibleTableRow(
        modifier = modifier,
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
                    SingleComponent(
                        viewType = viewType,
                        isTrailing = false,
                        maskState = MaskStateConfig.Override(false)
                    )

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
                    SingleComponent(
                        viewType = viewType,
                        isTrailing = true,
                        maskState = maskState
                    )

                    if (index < trailingComponents.lastIndex) {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                    }
                }
            }
        },
        contentEnd = {
            if (endIcon != ImageResource.None) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
                Image(endIcon)
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
    isTrailing: Boolean,
    maskState: MaskStateConfig
) {
    when (viewType) {
        is ViewType.Text -> {
            StyledText(
                text = viewType.value,
                style = viewType.style,
                textAlign = if (isTrailing) TextAlign.End else TextAlign.Start,
                maskState = maskState
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
        endIcon = Icons.Filled.Email.withTint(AppColors.primary),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCustomTableRowDark_Summary_SmallTag() {
    PreviewCustomTableRow_Summary_SmallTag()
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCustomTableRowDark_Summary_StackedIcon() {
    PreviewCustomTableRow_Summary_StackedIcon()
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCustomTableRowDark_Summary_SingleIcon() {
    PreviewCustomTableRow_Summary_SingleIcon()
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCustomTableRowDark() {
    PreviewCustomTableRow()
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCustomTableRowDark_Key_MultiValue() {
    PreviewCustomTableRow_Key_MultiValue()
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCustomTableRowDark_KeyValue() {
    PreviewCustomTableRow_KeyValue()
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCustomTableRowDark_Tag() {
    PreviewCustomTableRow_Tag()
}
