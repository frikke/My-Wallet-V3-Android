package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.tag.TagsRow
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun BalanceTableRow(
    titleStart: AnnotatedString,
    titleEnd: AnnotatedString? = null,
    bodyStart: AnnotatedString,
    bodyEnd: AnnotatedString? = null,
    startImageResource: ImageResource,
    isInlineTags: Boolean = false,
    tags: List<TagViewState>,
    onClick: () -> Unit
) {

    TableRow(
        content = {
            Column(
                modifier = Modifier
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
                        Text(
                            text = bodyStart,
                            style = AppTheme.typography.paragraph1,
                            modifier = Modifier
                                .wrapContentSize()
                                .align(Alignment.CenterVertically),
                            color = AppTheme.colors.body
                        )
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
        contentStart = {
            Image(
                imageResource = startImageResource,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 16.dp)
                    .size(24.dp),
                coilImageBuilderScope = null
            )
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
