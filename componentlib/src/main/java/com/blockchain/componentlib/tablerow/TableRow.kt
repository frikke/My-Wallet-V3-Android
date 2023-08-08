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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.conditional

@Composable
private fun TableRow(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    content: @Composable RowScope.() -> Unit,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    contentEnd: @Composable (RowScope.() -> Unit)? = null,
    contentBottom: @Composable (() -> Unit)? = null,
    onContentClicked: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    backgroundShape: Shape = RectangleShape,
    contentAlpha: Float = 1F
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .conditional(onContentClicked != null) {
                clickable { onContentClicked?.invoke() }
            }
            .background(backgroundColor, backgroundShape)
            .padding(paddingValues)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            contentStart?.invoke(this)
            Row(
                modifier = Modifier.weight(weight = 1F, fill = true),
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
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
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    backgroundShape: Shape = RectangleShape,
    contentAlpha: Float = 1F
) {
    TableRow(
        modifier = modifier,
        paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
        content = content,
        contentStart = contentStart,
        contentEnd = contentEnd,
        contentBottom = contentBottom,
        onContentClicked = onContentClicked,
        backgroundColor = backgroundColor,
        backgroundShape = backgroundShape,
        contentAlpha = contentAlpha
    )
}

@Composable
fun FlexibleTableRow(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    content: @Composable RowScope.() -> Unit,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    contentEnd: @Composable (RowScope.() -> Unit)? = null,
    contentBottom: @Composable (() -> Unit)? = null,
    onContentClicked: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    backgroundShape: Shape = RectangleShape
) {
    TableRow(
        modifier = modifier,
        paddingValues = paddingValues,
        content = content,
        contentStart = contentStart,
        contentEnd = contentEnd,
        contentBottom = contentBottom,
        onContentClicked = onContentClicked,
        backgroundColor = backgroundColor,
        backgroundShape = backgroundShape
    )
}
