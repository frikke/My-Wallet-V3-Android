package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.AppTheme

@Composable
private fun TableRow(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    content: @Composable RowScope.() -> Unit,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    contentEnd: @Composable (RowScope.() -> Unit)? = null,
    contentBottom: @Composable (() -> Unit)? = null,
    onContentClicked: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colors.background
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .run {
                if (onContentClicked != null) {
                    clickable { onContentClicked() }
                } else {
                    this
                }
            }
            .background(backgroundColor)
            .padding(paddingValues)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            contentStart?.invoke(this)
            content()
            contentEnd?.invoke(this)
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            contentBottom?.invoke()
        }
    }
}

@Composable
fun TableRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    contentEnd: @Composable (RowScope.() -> Unit)? = null,
    contentBottom: @Composable (() -> Unit)? = null,
    onContentClicked: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colors.background
) {
    TableRow(
        modifier = modifier,
        paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
        content = content,
        contentStart = contentStart,
        contentEnd = contentEnd,
        contentBottom = contentBottom,
        onContentClicked = onContentClicked,
        backgroundColor = backgroundColor
    )
}

@Composable
fun FlexibleTableRow(
    paddingValues: PaddingValues,
    content: @Composable RowScope.() -> Unit,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    contentEnd: @Composable (RowScope.() -> Unit)? = null,
    contentBottom: @Composable (() -> Unit)? = null,
    onContentClicked: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colors.background
) {
    TableRow(
        paddingValues = paddingValues,
        content = content,
        contentStart = contentStart,
        contentEnd = contentEnd,
        contentBottom = contentBottom,
        onContentClicked = onContentClicked,
        backgroundColor = backgroundColor
    )
}
