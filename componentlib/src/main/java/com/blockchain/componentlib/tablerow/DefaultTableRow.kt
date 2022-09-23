package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
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

@Composable
fun DefaultTableRow(
    primaryText: String,
    onClick: () -> Unit,
    secondaryText: String? = null,
    paragraphText: String? = null,
    endText: String? = null,
    tags: List<TagViewState>? = null,
    endTag: TagViewState? = null,
    startImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = ImageResource.Local(
        id = R.drawable.ic_chevron_end,
        contentDescription = null
    ),
    backgroundColor: Color = AppTheme.colors.background
) {
    DefaultTableRow(
        primaryText = buildAnnotatedString { append(primaryText) },
        onClick = onClick,
        secondaryText = secondaryText?.let { buildAnnotatedString { append(it) } },
        paragraphText = paragraphText?.let { buildAnnotatedString { append(it) } },
        endText = endText?.let { buildAnnotatedString { append(it) } },
        tags = tags,
        endTag = endTag,
        startImageResource = startImageResource,
        endImageResource = endImageResource,
        backgroundColor = backgroundColor
    )
}

@Composable
fun DefaultTableRow(
    primaryText: AnnotatedString,
    onClick: () -> Unit,
    secondaryText: AnnotatedString? = null,
    paragraphText: AnnotatedString? = null,
    endText: AnnotatedString? = null,
    tags: List<TagViewState>? = null,
    endTag: TagViewState? = null,
    startImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = ImageResource.Local(
        id = R.drawable.ic_chevron_end,
        contentDescription = null
    ),
    backgroundColor: Color = AppTheme.colors.background
) {
    TableRow(
        contentStart = {
            Image(
                imageResource = startImageResource,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimensionResource(R.dimen.standard_spacing))
            )
        },
        content = {
            val startPadding = if (startImageResource != ImageResource.None) {
                dimensionResource(R.dimen.medium_spacing)
            } else {
                dimensionResource(R.dimen.zero_spacing)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = startPadding, end = 8.dp)
            ) {
                Text(
                    text = primaryText,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )
                if (secondaryText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = secondaryText,
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                }
            }
        },
        contentEnd = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (endText != null) {
                    Text(
                        text = endText,
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.title
                    )

                    Spacer(Modifier.height(8.dp))
                }

                if (endTag != null) {
                    TagsRow(listOf(endTag))
                } else {
                    Image(
                        imageResource = endImageResource,
                        modifier = Modifier.requiredSizeIn(
                            maxWidth = dimensionResource(R.dimen.standard_spacing),
                            maxHeight = dimensionResource(R.dimen.standard_spacing),
                        ),
                    )
                }
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
        backgroundColor = backgroundColor
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
                endTag = TagViewState("Complete", TagType.Success()),
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
                    iconTintColour = R.color.paletteBasePrimary,
                    backgroundColour = R.color.paletteBasePrimaryMuted,
                    contentDescription = null
                )
            )
        }
    }
}
