package com.blockchain.componentlib.tablerow.generic

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.FlexibleTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.tag.TagsRow
import com.blockchain.componentlib.theme.AppTheme

@Composable
private fun StyledText(
    text: String,
    style: ViewStyle.TextStyle
) {
    Text(
        text = text,
        style = style.style.copy(
            textDecoration = style.textDecoration()
        ),
        color = style.color,
    )
}

@Composable
private fun GenericTableRow(
    leadingImagePrimary: ImageResource = ImageResource.None,
    leadingImageSecondary: ImageResource = ImageResource.None,
    leadingComponents: List<ViewType>,
    trailingComponents: List<ViewType>,
    onClick: (() -> Unit)? = null
) {
    FlexibleTableRow(
        paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
        contentStart = {
            if (leadingImagePrimary != ImageResource.None) {
                val stackedIconPadding = if (leadingImageSecondary != ImageResource.None) {
                    2.dp // 2 extra to account for secondary icon
                } else {
                    AppTheme.dimensions.noSpacing
                }

                Box(
                    modifier = Modifier
                        .size(
                            AppTheme.dimensions.standardSpacing + stackedIconPadding
                        )
                ) {
                    Image(imageResource = leadingImagePrimary)

                    if (leadingImageSecondary != ImageResource.None) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(AppTheme.dimensions.verySmallSpacing + stackedIconPadding)
                                .background(
                                    color = AppTheme.colors.background,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                imageResource = leadingImageSecondary
                            )
                        }
                    }
                }
            }
        },
        content = {
            if (leadingImagePrimary != ImageResource.None) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            }

            Column(modifier = Modifier.weight(1F)) {
                leadingComponents.forEachIndexed { index, viewType ->
                    SingleComponent(viewType)

                    if (index < leadingComponents.lastIndex) {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                trailingComponents.forEachIndexed { index, viewType ->
                    SingleComponent(viewType)

                    if (index < trailingComponents.lastIndex) {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                    }
                }
            }
        },
        onContentClicked = onClick
    )
}

/**
 * for drawable res images
 */
@Composable
fun GenericTableRow(
    @DrawableRes leadingImagePrimaryRes: Int,
    @DrawableRes leadingImageSecondaryRes: Int? = null,
    leadingComponents: List<ViewType>,
    trailingComponents: List<ViewType>,
    onClick: () -> Unit
) {
    GenericTableRow(
        leadingImagePrimary = ImageResource.Local(
            id = leadingImagePrimaryRes,
            shape = CircleShape,
            size = AppTheme.dimensions.standardSpacing
        ),
        leadingImageSecondary = leadingImageSecondaryRes?.let {
            ImageResource.Local(
                id = leadingImageSecondaryRes,
                shape = CircleShape,
                size = AppTheme.dimensions.verySmallSpacing
            )
        } ?: ImageResource.None,
        leadingComponents = leadingComponents,
        trailingComponents = trailingComponents,
        onClick = onClick
    )
}

/**
 * for remote url images
 */
@Composable
fun GenericTableRow(
    leadingImagePrimaryUrl: String? = null,
    leadingImageSecondaryUrl: String? = null,
    leadingComponents: List<ViewType>,
    trailingComponents: List<ViewType>,
    onClick: (() -> Unit)? = null
) {
    GenericTableRow(
        leadingImagePrimary = leadingImagePrimaryUrl?.let {
            ImageResource.Remote(
                url = leadingImagePrimaryUrl,
                shape = CircleShape,
                size = AppTheme.dimensions.standardSpacing
            )
        } ?: ImageResource.None,
        leadingImageSecondary = leadingImageSecondaryUrl?.let {
            ImageResource.Remote(
                url = leadingImageSecondaryUrl,
                shape = CircleShape,
                size = AppTheme.dimensions.verySmallSpacing
            )
        } ?: ImageResource.None,
        leadingComponents = leadingComponents,
        trailingComponents = trailingComponents,
        onClick = onClick
    )
}

@Composable
fun GenericTableRow(
    leadingComponents: List<ViewType>,
    trailingComponents: List<ViewType>,
    onClick: () -> Unit
) {
    GenericTableRow(
        leadingImagePrimary = ImageResource.None,
        leadingImageSecondary = ImageResource.None,
        leadingComponents = leadingComponents,
        trailingComponents = trailingComponents,
        onClick = onClick
    )
}

@Composable
private fun SingleComponent(viewType: ViewType) {
    when (viewType) {
        is ViewType.Text -> {
            StyledText(
                text = viewType.value,
                style = viewType.style
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

        ViewType.Unknown -> { /* n/a */
        }
    }
}

@Preview
@Composable
private fun PreviewGenericTableRow_Summary_SingleIcon() {
    GenericTableRow(
        leadingImagePrimaryRes = R.drawable.ic_two_circle,
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
private fun PreviewGenericTableRow_Summary_StackedIcon() {
    GenericTableRow(
        leadingImagePrimaryRes = R.drawable.ic_two_circle,
        leadingImageSecondaryRes = R.drawable.ic_eth,
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
private fun PreviewGenericTableRow() {
    GenericTableRow(
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
private fun PreviewGenericTableRow_Key_MultiValue() {
    GenericTableRow(
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
private fun PreviewGenericTableRow_KeyValue() {
    GenericTableRow(
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
private fun PreviewGenericTableRow_Tag() {
    GenericTableRow(
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
