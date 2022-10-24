package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.tag.TagsRow
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700

sealed interface StyledTableRowField {
    @get:Composable
    val color: Color
    val strikeThrough: Boolean

    object Primary : StyledTableRowField {
        override val color @Composable get() = AppTheme.colors.title
        override val strikeThrough = false
    }

    data class Muted(override val strikeThrough: Boolean = false) : StyledTableRowField {
        override val color @Composable get() = Grey700
    }

    object Info : StyledTableRowField {
        override val color @Composable get() = Color(0xFFDE0082) // todo(othamn) superapp theme
        override val strikeThrough = false
    }

    object Warning : StyledTableRowField {
        override val color @Composable get() = AppTheme.colors.warning
        override val strikeThrough = false
    }

    object Error : StyledTableRowField {
        override val color @Composable get() = AppTheme.colors.error
        override val strikeThrough = false
    }

    object Success : StyledTableRowField {
        override val color @Composable get() = AppTheme.colors.success
        override val strikeThrough = false
    }
}

private fun StyledTableRowField.textDecoration(): TextDecoration {
    return if (strikeThrough) {
        TextDecoration.LineThrough
    } else {
        TextDecoration.None
    }
}

@Composable
private fun StyledText(
    text: String,
    style: TextStyle,
    customStyle: StyledTableRowField
) {
    Text(
        text = text,
        style = style.copy(
            textDecoration = customStyle.textDecoration()
        ),
        color = customStyle.color,
    )
}

@Composable
private fun StyledTableRow(
    topStartText: String,
    topStartTextStyle: StyledTableRowField = StyledTableRowField.Primary,
    bottomStartText: String? = null,
    bottomStartTextStyle: StyledTableRowField = StyledTableRowField.Muted(strikeThrough = false),
    topEndText: String? = null,
    topEndTextStyle: StyledTableRowField = StyledTableRowField.Primary,
    bottomEndText: String? = null,
    bottomEndTextStyle: StyledTableRowField = StyledTableRowField.Muted(strikeThrough = false),
    tag: TagViewState? = null,
    startMainImageResource: ImageResource = ImageResource.None,
    startSecondaryImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = ImageResource.None,
    onClick: (() -> Unit)? = null
) {
    FlexibleTableRow(
        paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
        contentStart = {

            if (startMainImageResource != ImageResource.None) {
                val stackedIconPadding = if (startSecondaryImageResource != ImageResource.None) {
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
                    Image(imageResource = startMainImageResource)

                    if (startSecondaryImageResource != ImageResource.None) {
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
                                imageResource = startSecondaryImageResource
                            )
                        }
                    }
                }
            }
        },
        content = {
            if (startMainImageResource != ImageResource.None) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            }

            Column(modifier = Modifier.weight(1F)) {
                StyledText(
                    text = topStartText,
                    style = AppTheme.typography.paragraph2,
                    customStyle = topStartTextStyle
                )

                bottomStartText?.let {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                    StyledText(
                        text = bottomStartText,
                        style = AppTheme.typography.caption1,
                        customStyle = bottomStartTextStyle
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                topEndText?.let {
                    StyledText(
                        text = topEndText,
                        style = AppTheme.typography.paragraph2,
                        customStyle = topEndTextStyle
                    )
                }

                bottomEndText?.let {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                    StyledText(
                        text = bottomEndText,
                        style = AppTheme.typography.caption1,
                        customStyle = bottomEndTextStyle
                    )
                }
            }
        },
        contentEnd = {
            tag?.let {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                TagsRow(listOf(tag))
            }

            if (endImageResource != ImageResource.None) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                Image(
                    imageResource = endImageResource,
                    modifier = Modifier.requiredSizeIn(
                        maxWidth = AppTheme.dimensions.standardSpacing,
                        maxHeight = AppTheme.dimensions.standardSpacing,
                    ),
                )
            }
        },
        onContentClicked = onClick
    )
}

@Composable
fun StyledTableRow(
    topStartText: String,
    topStartTextStyle: StyledTableRowField = StyledTableRowField.Primary,
    bottomStartText: String? = null,
    bottomStartTextStyle: StyledTableRowField = StyledTableRowField.Muted(strikeThrough = false),
    topEndText: String? = null,
    topEndTextStyle: StyledTableRowField = StyledTableRowField.Primary,
    bottomEndText: String? = null,
    bottomEndTextStyle: StyledTableRowField = StyledTableRowField.Muted(strikeThrough = false),
    tag: TagViewState? = null,
    startMainImageUrl: String? = null,
    startSecondaryImageUrl: String? = null,
    endImageResource: ImageResource = ImageResource.None,
    onClick: (() -> Unit)? = null
) {
    StyledTableRow(
        topStartText = topStartText,
        topStartTextStyle = topStartTextStyle,
        bottomStartText = bottomStartText,
        bottomStartTextStyle = bottomStartTextStyle,
        topEndText = topEndText,
        topEndTextStyle = topEndTextStyle,
        bottomEndText = bottomEndText,
        bottomEndTextStyle = bottomEndTextStyle,
        tag = tag,
        startMainImageResource = startMainImageUrl?.let {
            ImageResource.Remote(
                url = startMainImageUrl,
                shape = CircleShape,
                size = AppTheme.dimensions.standardSpacing
            )
        } ?: ImageResource.None,
        startSecondaryImageResource = startSecondaryImageUrl?.let {
            ImageResource.Remote(
                url = startSecondaryImageUrl,
                shape = CircleShape,
                size = AppTheme.dimensions.verySmallSpacing
            )
        } ?: ImageResource.None,
        endImageResource = endImageResource,
        onClick = onClick
    )
}

@Composable
fun StyledTableRow(
    topStartText: String,
    topStartTextStyle: StyledTableRowField = StyledTableRowField.Primary,
    bottomStartText: String? = null,
    bottomStartTextStyle: StyledTableRowField = StyledTableRowField.Muted(strikeThrough = false),
    topEndText: String? = null,
    topEndTextStyle: StyledTableRowField = StyledTableRowField.Primary,
    bottomEndText: String? = null,
    bottomEndTextStyle: StyledTableRowField = StyledTableRowField.Muted(strikeThrough = false),
    tag: TagViewState? = null,
    startMainImageRes: Int,
    startSecondaryImageRes: Int? = null,
    endImageResource: ImageResource = ImageResource.None,
    onClick: (() -> Unit)? = null
) {
    StyledTableRow(
        topStartText = topStartText,
        topStartTextStyle = topStartTextStyle,
        bottomStartText = bottomStartText,
        bottomStartTextStyle = bottomStartTextStyle,
        topEndText = topEndText,
        topEndTextStyle = topEndTextStyle,
        bottomEndText = bottomEndText,
        bottomEndTextStyle = bottomEndTextStyle,
        tag = tag,
        startMainImageResource = ImageResource.Local(
            id = startMainImageRes,
            shape = CircleShape,
            size = AppTheme.dimensions.standardSpacing
        ),
        startSecondaryImageResource = startSecondaryImageRes?.let {
            ImageResource.Local(
                id = startSecondaryImageRes,
                shape = CircleShape,
                size = AppTheme.dimensions.verySmallSpacing
            )
        } ?: ImageResource.None,
        endImageResource = endImageResource,
        onClick = onClick
    )
}

@Composable
fun StyledTableRow(
    topStartText: String,
    topStartTextStyle: StyledTableRowField = StyledTableRowField.Primary,
    bottomStartText: String? = null,
    bottomStartTextStyle: StyledTableRowField = StyledTableRowField.Muted(strikeThrough = false),
    topEndText: String? = null,
    topEndTextStyle: StyledTableRowField = StyledTableRowField.Primary,
    bottomEndText: String? = null,
    bottomEndTextStyle: StyledTableRowField = StyledTableRowField.Muted(strikeThrough = false),
    tag: TagViewState? = null,
    endImageResource: ImageResource = ImageResource.None,
    onClick: (() -> Unit)? = null
) {
    StyledTableRow(
        topStartText = topStartText,
        topStartTextStyle = topStartTextStyle,
        bottomStartText = bottomStartText,
        bottomStartTextStyle = bottomStartTextStyle,
        topEndText = topEndText,
        topEndTextStyle = topEndTextStyle,
        bottomEndText = bottomEndText,
        bottomEndTextStyle = bottomEndTextStyle,
        tag = tag,
        startMainImageResource = ImageResource.None,
        startSecondaryImageResource = ImageResource.None,
        endImageResource = endImageResource,
        onClick = onClick
    )
}

@Composable
fun KeyValueStyledTableRow(
    keyText: String,
    keyTextStyle: StyledTableRowField = StyledTableRowField.Muted(),
    valueText: String,
    valueTextStyle: StyledTableRowField = StyledTableRowField.Primary,
    endImageResource: ImageResource = ImageResource.None,
    onClick: (() -> Unit)? = null
) {
    StyledTableRow(
        topStartText = keyText,
        topStartTextStyle = keyTextStyle,
        topEndText = valueText,
        topEndTextStyle = valueTextStyle,
        endImageResource = endImageResource,
        onClick = onClick
    )
}

@Composable
fun KeyValuesStyledTableRow(
    keyText: String,
    keyTextStyle: StyledTableRowField = StyledTableRowField.Muted(),
    value1Text: String,
    value1TextStyle: StyledTableRowField = StyledTableRowField.Primary,
    value2Text: String,
    value2TextStyle: StyledTableRowField = StyledTableRowField.Muted(),
    endImageResource: ImageResource = ImageResource.None,
    onClick: (() -> Unit)? = null
) {
    StyledTableRow(
        topStartText = keyText,
        topStartTextStyle = keyTextStyle,
        topEndText = value1Text,
        topEndTextStyle = value1TextStyle,
        bottomEndText = value2Text,
        bottomEndTextStyle = value2TextStyle,
        endImageResource = endImageResource,
        onClick = onClick
    )
}

@Preview
@Composable
private fun StyledTableRow_All4_default_mainicon() {
    AppTheme {
        AppSurface {
            StyledTableRow(
                topStartText = "Sent Ethereum",
                bottomStartText = "June 14",
                bottomStartTextStyle = StyledTableRowField.Muted(),
                topEndText = "-10.00",
                bottomEndText = "-0.00893208 ETH",
                bottomEndTextStyle = StyledTableRowField.Muted(),
                startMainImageRes = R.drawable.ic_two_circle,
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun StyledTableRow_All4_default_stackedicon() {
    AppTheme {
        AppSurface {
            StyledTableRow(
                topStartText = "Sent Ethereum",
                bottomStartText = "June 14",
                bottomStartTextStyle = StyledTableRowField.Muted(),
                topEndText = "-10.00",
                bottomEndText = "-0.00893208 ETH",
                bottomEndTextStyle = StyledTableRowField.Muted(),
                startMainImageRes = R.drawable.ic_two_circle,
                startSecondaryImageRes = R.drawable.ic_eth,
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun StyledTableRow_All4_default() {
    AppTheme {
        AppSurface {
            StyledTableRow(
                topStartText = "Sent Ethereum",
                bottomStartText = "June 14",
                bottomStartTextStyle = StyledTableRowField.Muted(),
                topEndText = "-10.00",
                bottomEndText = "-0.00893208 ETH",
                bottomEndTextStyle = StyledTableRowField.Muted(),
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun StyledTableRow_All4_warning() {
    AppTheme {
        AppSurface {
            StyledTableRow(
                topStartText = "Sent Ethereum",
                bottomStartText = "canceled",
                bottomStartTextStyle = StyledTableRowField.Warning,
                topEndText = "-10.00",
                topEndTextStyle = StyledTableRowField.Muted(true),
                bottomEndText = "-0.00893208 ETH",
                bottomEndTextStyle = StyledTableRowField.Muted(true),
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun StyledTableRow_All4_muted() {
    AppTheme {
        AppSurface {
            StyledTableRow(
                topStartText = "Sent Ethereum",
                topStartTextStyle = StyledTableRowField.Muted(),
                bottomStartText = "June 14",
                bottomStartTextStyle = StyledTableRowField.Muted(),
                topEndText = "-10.00",
                topEndTextStyle = StyledTableRowField.Muted(),
                bottomEndText = "-0.00893208 ETH",
                bottomEndTextStyle = StyledTableRowField.Muted(),
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun KeyValueStyledTableRow_default() {
    AppTheme {
        AppSurface {
            KeyValueStyledTableRow(
                keyText = "Purchase",
                valueText = "0.00503823 BTC"
            )
        }
    }
}

@Preview
@Composable
private fun KeyValueStyledTableRow_default_chevron() {
    AppTheme {
        AppSurface {
            KeyValueStyledTableRow(
                keyText = "Note",
                valueText = "No notes",
                valueTextStyle = StyledTableRowField.Muted(),
                endImageResource = ImageResource.Local(R.drawable.ic_chevron_end)
            )
        }
    }
}

@Preview
@Composable
private fun KeyValueStyledTableRow_success() {
    AppTheme {
        AppSurface {
            KeyValueStyledTableRow(
                keyText = "Fees",
                valueText = "Free",
                valueTextStyle = StyledTableRowField.Success
            )
        }
    }
}

@Preview
@Composable
private fun KeyValuesStyledTableRow_default() {
    AppTheme {
        AppSurface {
            KeyValuesStyledTableRow(
                keyText = "Fees",
                value1Text = "6.17",
                value2Text = "0.00503823 BTC"
            )
        }
    }
}

@Preview
@Composable
private fun StyledTableRow_ValueTag() {
    AppTheme {
        AppSurface {
            StyledTableRow(
                topStartText = "Status",
                topStartTextStyle = StyledTableRowField.Muted(),
                tag = TagViewState("Failed", TagType.Error()),
                endImageResource = ImageResource.None,
                onClick = {}
            )
        }
    }
}
