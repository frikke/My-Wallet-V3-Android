package com.blockchain.componentlib.tablerow

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
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
    tags: List<TagViewState>? = null,
    @DrawableRes endIconResId: Int = R.drawable.ic_chevron_end,
    showEndIcon: Boolean = true
) {
    TableRow(
        content = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = primaryText,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                }
            }
        },
        contentEnd = {
            if (showEndIcon) {
                Image(
                    painter = painterResource(id = endIconResId),
                    contentDescription = null,
                    modifier = Modifier.requiredSizeIn(
                        maxWidth = 24.dp,
                        maxHeight = 24.dp,
                    ),
                )
            }
        },
        onContentClicked = onClick,
        contentBottom = {
            Column {
                if (paragraphText != null) {
                    Text(
                        text = paragraphText,
                        style = AppTheme.typography.caption1,
                        color = AppTheme.colors.body,
                        modifier = Modifier
                            .padding(
                                top = 4.dp,
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
        }
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
fun DefaultTableRow_Basic_Chevron() {
    AppTheme(darkTheme = true) {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                onClick = {},
                showEndIcon = true
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
                showEndIcon = false
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
                        type = TagType.Success
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
                        type = TagType.Success
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning
                    )
                )
            )
        }
    }
}

@Preview
@Composable
fun DefaultTableRow_Long_Tag() {
    AppTheme() {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                paragraphText = "This is a long paragraph which wraps, ".repeat(5),
                onClick = {},
                tags = listOf(
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success
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
                        type = TagType.Success
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning
                    )
                )
            )
        }
    }
}
