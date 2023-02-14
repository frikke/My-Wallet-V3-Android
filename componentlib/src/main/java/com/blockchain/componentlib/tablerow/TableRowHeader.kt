package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.extensions.safeLet

@Composable
fun TableRowHeader(
    title: String,
    actionTitle: String? = null,
    actionOnClick: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = AppTheme.typography.body2,
            color = Grey700
        )

        Spacer(modifier = Modifier.weight(1f))

        safeLet(actionTitle, actionOnClick) { text, onClick ->
            Text(
                modifier = Modifier.clickableNoEffect(onClick),
                text = text,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTableRowHeader() {
    TableRowHeader(
        title = "Assets",
        actionTitle = "See all",
        actionOnClick = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewTableRowHeader_NoAction() {
    TableRowHeader(
        title = "Assets"
    )
}
