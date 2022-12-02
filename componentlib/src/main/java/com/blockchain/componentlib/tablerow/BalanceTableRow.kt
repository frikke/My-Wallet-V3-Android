package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
fun BalanceTableRow(
    titleStart: AnnotatedString,
    titleEnd: AnnotatedString? = null,
    bodyStart: AnnotatedString? = null,
    bodyEnd: AnnotatedString? = null,
    startImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = ImageResource.None,
    postStartTitleImageResource: ImageResource = ImageResource.None,
    postStartTitleImageResourceOnClick: () -> Unit = {},
    isInlineTags: Boolean = false,
    tags: List<TagViewState> = emptyList(),
    onClick: () -> Unit = {}
) {

    TableRow(
        contentStart = {
            Image(
                imageResource = startImageResource,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimensionResource(R.dimen.standard_spacing)),
                defaultShape = RoundedCornerShape(2.dp)
            )
        },
        content = {
            val startPadding = if (startImageResource != ImageResource.None) {
                dimensionResource(R.dimen.small_spacing)
            } else {
                dimensionResource(R.dimen.zero_spacing)
            }

            val endPadding = if (endImageResource != ImageResource.None) {
                dimensionResource(R.dimen.small_spacing)
            } else {
                dimensionResource(R.dimen.zero_spacing)
            }
            Column(
                modifier = Modifier
                    .padding(start = startPadding, end = endPadding)
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Text(
                        text = titleStart,
                        style = AppTheme.typography.body2,
                        modifier = if (postStartTitleImageResource == ImageResource.None) {
                            Modifier.weight(1f)
                        } else {
                            Modifier.wrapContentWidth()
                        },
                        textAlign = TextAlign.Start,
                        color = AppTheme.colors.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (postStartTitleImageResource != ImageResource.None) {
                        Image(
                            imageResource = postStartTitleImageResource,
                            modifier = Modifier
                                .padding(AppTheme.dimensions.smallestSpacing)
                                .clickable(onClick = postStartTitleImageResourceOnClick)
                                .size(AppTheme.dimensions.smallSpacing)
                        )
                    }

                    if (titleEnd != null) {
                        Text(
                            text = titleEnd,
                            style = AppTheme.typography.body2,
                            modifier = if (postStartTitleImageResource == ImageResource.None) {
                                Modifier.wrapContentSize()
                            } else {
                                Modifier.weight(1f)
                            },
                            textAlign = TextAlign.End,
                            color = AppTheme.colors.title
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Row(
                        modifier = Modifier.weight(1f)
                    ) {
                        bodyStart?.let {
                            Text(
                                text = bodyStart,
                                style = AppTheme.typography.paragraph1,
                                modifier = Modifier
                                    .wrapContentSize()
                                    .align(Alignment.CenterVertically),
                                color = AppTheme.colors.body
                            )
                        }
                        if (isInlineTags) {
                            TagsRow(
                                tags = tags,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    if (bodyEnd != null) {
                        Text(
                            text = bodyEnd,
                            style = AppTheme.typography.paragraph1,
                            modifier = Modifier.wrapContentSize(),
                            color = AppTheme.colors.body
                        )
                    }
                }
            }
        },
        contentEnd = {
            if (endImageResource != ImageResource.None) {
                Image(
                    imageResource = endImageResource,
                    modifier = Modifier.requiredSizeIn(
                        maxWidth = dimensionResource(R.dimen.standard_spacing),
                        maxHeight = dimensionResource(R.dimen.standard_spacing),
                    )
                )
            }
        },
        contentBottom = {
            if (!isInlineTags && tags.isNotEmpty()) {
                TagsRow(
                    tags = tags,
                    modifier = Modifier.padding(
                        top = if (bodyStart != null) {
                            AppTheme.dimensions.tinySpacing
                        } else {
                            0.dp
                        },
                        start = AppTheme.dimensions.hugeSpacing
                    )
                )
            }
        },
        onContentClicked = onClick
    )
}

@Preview
@Composable
fun BalanceTableRow_Local_ImageStart() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString { append("Some title here") },
                bodyStart = buildAnnotatedString { append("Some body here") },
                onClick = {},
                startImageResource = ImageResource.Local(
                    id = R.drawable.ic_blockchain,
                ),
                tags = emptyList()
            )
        }
    }
}

@Preview
@Composable
fun BalanceTableRow_Local_ImageEnd() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString { append("Some title here") },
                bodyStart = buildAnnotatedString { append("Some body here") },
                onClick = {},
                endImageResource = ImageResource.Local(
                    id = R.drawable.ic_blockchain,
                ),
                tags = emptyList()
            )
        }
    }
}

@Preview
@Composable
fun BalanceTableRow_Texts() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString { append("Some title here") },
                bodyStart = buildAnnotatedString { append("Some body here") },
                titleEnd = buildAnnotatedString { append("100 BTC") },
                bodyEnd = buildAnnotatedString { append("$100") },
                onClick = {},
                tags = emptyList(),
                startImageResource = ImageResource.None
            )
        }
    }
}

@Preview
@Composable
fun BalanceTableRow_TitleStart_No_Body_Start() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString { append("Some title here") },
                titleEnd = buildAnnotatedString { append("100 BTC") },
                bodyEnd = buildAnnotatedString { append("$100") },
                onClick = {},
                tags = emptyList(),
                startImageResource = ImageResource.None
            )
        }
    }
}

@Preview
@Composable
fun BalanceTableRow_TitleStart_Tags() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString { append("Some title here") },
                titleEnd = buildAnnotatedString { append("100 BTC") },
                bodyEnd = buildAnnotatedString { append("$100") },
                bodyStart = buildAnnotatedString { append("Some body here") },
                onClick = {},
                tags = listOf(TagViewState("One", TagType.Default()), TagViewState("Two", TagType.Success())),
                startImageResource = ImageResource.Local(
                    id = R.drawable.ic_blockchain,
                ),
            )
        }
    }
}

@Preview
@Composable
fun BalanceTableRow_TitleStart_Tags_Inline() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString { append("Some title here") },
                titleEnd = buildAnnotatedString { append("100 BTC") },
                bodyEnd = buildAnnotatedString { append("$100") },
                bodyStart = buildAnnotatedString { append("Some body here") },
                onClick = {},
                tags = listOf(TagViewState("One", TagType.Default()), TagViewState("Two", TagType.Success())),
                isInlineTags = true,
                startImageResource = ImageResource.Local(
                    id = R.drawable.ic_blockchain,
                ),
            )
        }
    }
}

@Preview
@Composable
fun BalanceTableRow_TitleStart_Tags_NoBodyStart() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString { append("Some title here") },
                titleEnd = buildAnnotatedString { append("100 BTC") },
                bodyEnd = buildAnnotatedString { append("$100") },
                onClick = {},
                tags = listOf(TagViewState("One", TagType.Default()), TagViewState("Two", TagType.Success())),
                startImageResource = ImageResource.Local(
                    id = R.drawable.ic_blockchain,
                ),
            )
        }
    }
}

@Preview
@Composable
fun BalanceTableRow_PostTitleImageResource() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString { append("Some title here") },
                titleEnd = buildAnnotatedString { append("100 BTC") },
                bodyEnd = buildAnnotatedString { append("$100") },
                onClick = {},
                tags = listOf(TagViewState("One", TagType.Default()), TagViewState("Two", TagType.Success())),
                startImageResource = ImageResource.Local(
                    id = R.drawable.ic_blockchain,
                ),
                postStartTitleImageResource = ImageResource.Local(
                    id = R.drawable.ic_blockchain,
                ),
            )
        }
    }
}
