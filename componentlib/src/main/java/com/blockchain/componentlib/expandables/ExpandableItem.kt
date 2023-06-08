package com.blockchain.componentlib.expandables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockchain.componentlib.button.SmallOutlinedButton
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ExpandableItem(
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String,
    numLinesVisible: Int,
    textButtonToExpand: String,
    textButtonToCollapse: String,
    background: Color = AppTheme.colors.backgroundSecondary
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isExpandable by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(background, AppTheme.shapes.large)
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Column {
            title?.let {
                Text(
                    text = title,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            }

            Text(
                text = text,
                style = AppTheme.typography.paragraph1,
                onTextLayout = { textLayoutResult ->
                    isExpandable = textLayoutResult.hasVisualOverflow
                },
                maxLines = if (isExpanded) Int.MAX_VALUE else numLinesVisible,
                lineHeight = 23.sp,
                overflow = TextOverflow.Ellipsis,
                color = AppTheme.colors.title
            )

            if (isExpandable && !isExpanded || !isExpandable && isExpanded) {
                SmallOutlinedButton(
                    modifier = modifier.padding(top = 18.dp),
                    text = if (isExpanded) textButtonToCollapse else textButtonToExpand,
                    onClick = { isExpanded = !isExpanded }
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewExpandableItem() {
    ExpandableItem(
        title = "Description",
        text = "Kyoto Angels is a Collection of 10000 Kawaii Dolls Manufactured by Collection of 10000 Kawaii Dolls",
        numLinesVisible = 2,
        textButtonToExpand = "See More",
        textButtonToCollapse = "See Less"
    )
}
