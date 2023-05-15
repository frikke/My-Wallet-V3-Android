package com.blockchain.componentlib.expandables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.utils.clickableNoEffect

@Composable
fun ExpandableItemBordered(
    modifier: Modifier = Modifier,
    title: String,
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
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .border(
                width = AppTheme.dimensions.borderSmall,
                color = AppTheme.colors.light,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Column {
            Text(
                text = title,
                style = AppTheme.typography.body2,
                color = Grey700
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            Text(
                text = text,
                style = AppTheme.typography.body2,
                color = AppTheme.colors.body,
                onTextLayout = { textLayoutResult ->
                    isExpandable = textLayoutResult.hasVisualOverflow
                },
                maxLines = if (isExpanded) Int.MAX_VALUE else numLinesVisible,
                lineHeight = 23.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            if (isExpandable && !isExpanded || !isExpandable && isExpanded) {
                Text(
                    modifier = Modifier.clickableNoEffect {
                        isExpanded = !isExpanded
                    },
                    text = if (isExpanded) textButtonToCollapse else textButtonToExpand,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.primary
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewExpandableItemBordered() {
    ExpandableItemBordered(
        title = "Description",
        text = "Kyoto Angels is a Collection of 10000 Kawaii Dolls Manufactured by Collection of 10000 Kawaii Dolls",
        numLinesVisible = 2,
        textButtonToExpand = "See More",
        textButtonToCollapse = "See Less"
    )
}
