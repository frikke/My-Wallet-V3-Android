package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
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
    startImageResource: ImageResource,
    endImageResource: ImageResource = ImageResource.None,
    isInlineTags: Boolean = false,
    tags: List<TagViewState>,
    onClick: () -> Unit
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
                dimensionResource(R.dimen.medium_spacing)
            } else {
                dimensionResource(R.dimen.zero_spacing)
            }

            val endPadding = if (endImageResource != ImageResource.None) {
                dimensionResource(R.dimen.medium_spacing)
            } else {
                dimensionResource(R.dimen.zero_spacing)
            }
            Column(
                modifier = Modifier
                    .padding(start = startPadding, end = endPadding)
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                TableRowText(
                    startText = titleStart,
                    endText = titleEnd,
                    textStyle = AppTheme.typography.body2,
                    textColor = AppTheme.colors.title
                )

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
                    ),
                )
            }
        },
        contentBottom = {
            if (!isInlineTags && !tags.isNullOrEmpty()) {
                TagsRow(
                    tags = tags,
                    modifier = Modifier.padding(top = 8.dp, start = 40.dp)
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
