package com.blockchain.componentlib.expandables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockchain.componentlib.button.SmallMinimalButton

@Composable
fun ExpandableItem(
    modifier: Modifier = Modifier,
    text: String,
    numLinesVisible: Int,
    textButtonToExpand: String,
    textButtonToCollapse: String
) {

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isExpandable by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column {
            Text(
                text = text,
                onTextLayout = { textLayoutResult ->
                    isExpandable = textLayoutResult.hasVisualOverflow
                },
                maxLines = if (isExpanded) Int.MAX_VALUE else numLinesVisible,
                lineHeight = 23.sp,
                overflow = TextOverflow.Ellipsis
            )

            if (isExpandable && !isExpanded || !isExpandable && isExpanded) {
                SmallMinimalButton(
                    modifier = modifier.padding(top = 18.dp),
                    text = if (isExpanded) textButtonToCollapse else textButtonToExpand,
                    onClick = { isExpanded = !isExpanded }
                )
            }
        }
    }
}
