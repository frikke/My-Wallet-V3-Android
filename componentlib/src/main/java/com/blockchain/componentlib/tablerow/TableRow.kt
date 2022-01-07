package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun TableRow(
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
            .padding(horizontal = 24.dp, vertical = 16.dp)
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
