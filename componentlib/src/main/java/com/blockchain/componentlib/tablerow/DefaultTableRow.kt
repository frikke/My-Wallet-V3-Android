package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.tag.TagsRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.MediumHorizontalSpacer
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer

@Composable
fun DefaultTableRow(
    modifier: Modifier = Modifier,
    primaryText: String,
    onClick: (() -> Unit)?,
    secondaryText: String? = null,
    paragraphText: String? = null,
    endText: String? = null,
    tags: List<TagViewState>? = null,
    endTag: TagViewState? = null,
    startImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = Icons.ChevronRight.withTint(AppColors.body),
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    primaryTextColor: Color = AppTheme.colors.title,
    secondaryTextColor: Color = AppTheme.colors.body,
    contentAlpha: Float = 1F
) {
    DefaultTableRow(
        modifier = modifier,
        primaryText = buildAnnotatedString { append(primaryText) },
        onClick = onClick,
        secondaryText = secondaryText?.let { buildAnnotatedString { append(it) } },
        paragraphText = paragraphText?.let { buildAnnotatedString { append(it) } },
        endText = endText?.let { buildAnnotatedString { append(it) } },
        tags = tags,
        endTag = endTag,
        startImageResource = startImageResource,
        endImageResource = endImageResource,
        backgroundColor = backgroundColor,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        contentAlpha = contentAlpha
    )
}

@Composable
fun DefaultTableRow(
    modifier: Modifier = Modifier,
    startTitle: String,
    onClick: (() -> Unit)?,
    startByline: String? = null,
    paragraphText: String? = null,
    endTitle: String? = null,
    endByline: String? = null,
    tags: List<TagViewState>? = null,
    endTag: TagViewState? = null,
    startImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = ImageResource.None,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    backgroundShape: Shape = RectangleShape,
    titleColor: Color = AppTheme.colors.title,
    titleStyle: TextStyle = AppTheme.typography.body2,
    bylineColor: Color = AppTheme.colors.muted,
    bylineStyle: TextStyle = AppTheme.typography.paragraph1,
    contentAlpha: Float = 1F
) {
    DefaultTableRow(
        modifier = modifier,
        startTitle = buildAnnotatedString { append(startTitle) },
        onClick = onClick,
        startByline = startByline?.let { buildAnnotatedString { append(it) } },
        paragraphText = paragraphText?.let { buildAnnotatedString { append(it) } },
        endTitle = endTitle?.let { buildAnnotatedString { append(it) } },
        endByline = endByline?.let { buildAnnotatedString { append(it) } },
        tags = tags,
        endTag = endTag,
        startImageResource = startImageResource,
        endImageResource = endImageResource,
        backgroundColor = backgroundColor,
        backgroundShape = backgroundShape,
        titleColor = titleColor,
        titleStyle = titleStyle,
        bylineColor = bylineColor,
        bylineStyle = bylineStyle,
        contentAlpha = contentAlpha
    )
}

@Composable
fun DefaultTableRow(
    modifier: Modifier = Modifier,
    primaryText: AnnotatedString,
    onClick: (() -> Unit)?,
    secondaryText: AnnotatedString? = null,
    paragraphText: AnnotatedString? = null,
    endText: AnnotatedString? = null,
    tags: List<TagViewState>? = null,
    endTag: TagViewState? = null,
    startImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = Icons.ChevronRight.withTint(AppColors.muted),
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    primaryTextColor: Color = AppTheme.colors.title,
    secondaryTextColor: Color = AppTheme.colors.muted,
    contentAlpha: Float = 1F
) {
    DefaultTableRow(
        modifier = modifier,
        startTitle = primaryText,
        onClick = onClick,
        startByline = secondaryText,
        paragraphText = paragraphText,
        endTitle = endText,
        endByline = null,
        tags = tags,
        endTag = endTag,
        startImageResource = startImageResource,
        endImageResource = endImageResource,
        backgroundColor = backgroundColor,
        titleColor = primaryTextColor,
        bylineColor = secondaryTextColor,
        contentAlpha = contentAlpha
    )
}

@Composable
fun DefaultTableRow(
    modifier: Modifier = Modifier,
    startTitle: AnnotatedString,
    onClick: (() -> Unit)?,
    startByline: AnnotatedString? = null,
    paragraphText: AnnotatedString? = null,
    endTitle: AnnotatedString? = null,
    endByline: AnnotatedString? = null,
    tags: List<TagViewState>? = null,
    endTag: TagViewState? = null,
    startImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = ImageResource.None,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    backgroundShape: Shape = RectangleShape,
    titleColor: Color = AppTheme.colors.title,
    titleStyle: TextStyle = AppTheme.typography.body2,
    bylineColor: Color = AppTheme.colors.muted,
    bylineStyle: TextStyle = AppTheme.typography.paragraph1,
    contentAlpha: Float = 1F
) {
    TableRow(
        modifier = modifier,
        contentStart = {
            if (startImageResource !is ImageResource.None) {
                Image(
                    imageResource = startImageResource,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )

                MediumHorizontalSpacer()
            }
        },
        content = {
            Column {
                Text(
                    text = startTitle,
                    style = titleStyle,
                    color = titleColor
                )
                if (startByline != null) {
                    SmallestVerticalSpacer()
                    Text(
                        text = startByline,
                        style = bylineStyle,
                        color = bylineColor
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (endTitle != null) {
                    Text(
                        text = endTitle,
                        style = titleStyle,
                        color = titleColor
                    )
                }
                if (endByline != null) {
                    if (endTitle != null) SmallestVerticalSpacer()
                    Text(
                        text = endByline,
                        style = bylineStyle,
                        color = bylineColor
                    )
                }
            }
        },
        contentEnd = {
            if (endTag != null) {
                SmallHorizontalSpacer()
                TagsRow(listOf(endTag))
            }
            if (endImageResource != ImageResource.None) {
                SmallHorizontalSpacer()
                Image(
                    imageResource = endImageResource,
                    modifier = Modifier.requiredSizeIn(
                        maxWidth = dimensionResource(R.dimen.standard_spacing),
                        maxHeight = dimensionResource(R.dimen.standard_spacing)
                    )
                )
            }
        },
        onContentClicked = onClick,
        contentBottom = {
            val startPadding = if (startImageResource != ImageResource.None) 40.dp else 0.dp
            Column(Modifier.padding(start = startPadding)) {
                if (paragraphText != null) {
                    Text(
                        text = paragraphText,
                        style = AppTheme.typography.caption1,
                        color = AppTheme.colors.body,
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(R.dimen.smallest_spacing),
                                bottom = if (tags.isNullOrEmpty()) 0.dp else 8.dp
                            )
                    )
                }
                if (!tags.isNullOrEmpty()) {
                    TagsRow(
                        tags = tags,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        backgroundColor = backgroundColor,
        backgroundShape = backgroundShape,
        contentAlpha = contentAlpha
    )
}

@Preview
@Composable
fun DefaultTableRow_Basic() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_TwoLine() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_TwoLine_EndTag() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                onClick = {},
                endTag = TagViewState("Complete", TagType.Success())
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_TwoLine_EndTag_EndText() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                primaryText = "Coffee Beans Inc.",
                onClick = {},
                secondaryText = "Jun 21, 2022",
                endText = "$100.00",
                endTag = TagViewState("Completed", TagType.Success())
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_ThreeLine() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                startTitle = "Coffee Beans Inc.",
                onClick = {},
                endTitle = "Jun 21, 2022",
                endByline = "$100.00"
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_FourLine() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                startTitle = "Coffee Beans Inc.",
                startByline = "Jun 21, 2022",
                onClick = {},
                endTitle = "1.0 BTC",
                endByline = "$100.00"
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Basic_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Basic_No_Chevron() {
    AppTheme(darkTheme = true) {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                onClick = {},
                endImageResource = ImageResource.None
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_TwoLine_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Tag() {
    AppTheme() {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                onClick = {},
                tags = listOf(
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success()
                    )
                )
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Tag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                onClick = {},
                tags = listOf(
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success()
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning()
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success()
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning()
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success()
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning()
                    )
                )
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Long_Tag() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                paragraphText = "This is a long paragraph which wraps, ".repeat(5),
                onClick = {},
                tags = listOf(
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success()
                    )
                )
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Long_Tag_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                paragraphText = "This is a long paragraph which wraps, ".repeat(5),
                onClick = {},
                tags = listOf(
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success()
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning()
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success()
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning()
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success()
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning()
                    )
                )
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Local_ImageStart() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                onClick = {},
                startImageResource = ImageResource.Local(
                    id = R.drawable.carousel_rewards,
                    contentDescription = null
                )
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Local_ImageStart_EndTag() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                onClick = {},
                startImageResource = ImageResource.Local(
                    id = R.drawable.carousel_rewards,
                    contentDescription = null
                ),
                endTag = TagViewState("Complete", TagType.Success())
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Local_With_BackgroundImageStart() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                onClick = {},
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_blockchain,
                    iconColor = AppTheme.colors.primary,
                    backgroundColor = AppTheme.colors.primaryMuted,
                    contentDescription = null
                )
            )
        }
    }
}
