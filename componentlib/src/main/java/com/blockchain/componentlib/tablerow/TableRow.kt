package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppTheme

@Composable
private fun TableRow(
    paddingValues: PaddingValues,
    content: @Composable RowScope.() -> Unit,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    contentEnd: @Composable (RowScope.() -> Unit)? = null,
    contentBottom: @Composable (() -> Unit)? = null,
    onContentClicked: (() -> Unit)? = null
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .run {
                if (onContentClicked != null) {
                    clickable { onContentClicked() }
                } else {
                    this
                }
            }
            .background(AppTheme.colors.background)
            .padding(paddingValues)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
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
    content: @Composable RowScope.() -> Unit,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    contentEnd: @Composable (RowScope.() -> Unit)? = null,
    contentBottom: @Composable (() -> Unit)? = null,
    onContentClicked: (() -> Unit)? = null
) {
    TableRow(
        paddingValues = PaddingValues(
            horizontal = dimensionResource(R.dimen.standard_margin),
            vertical = dimensionResource(R.dimen.medium_margin)
        ),
        content = content,
        contentStart = contentStart,
        contentEnd = contentEnd,
        contentBottom = contentBottom,
        onContentClicked = onContentClicked
    )
}

@Composable
fun FlexibleTableRow(
    paddingValues: PaddingValues,
    content: @Composable RowScope.() -> Unit,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    contentEnd: @Composable (RowScope.() -> Unit)? = null,
    contentBottom: @Composable (() -> Unit)? = null,
    onContentClicked: (() -> Unit)? = null
) {
    TableRow(
        paddingValues = paddingValues,
        content = content,
        contentStart = contentStart,
        contentEnd = contentEnd,
        contentBottom = contentBottom,
        onContentClicked = onContentClicked
    )
}
