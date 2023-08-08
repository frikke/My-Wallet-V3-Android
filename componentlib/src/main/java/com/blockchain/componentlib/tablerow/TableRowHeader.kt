package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Fire
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.extensions.safeLet

@Composable
fun TableRowHeader(
    title: String,
    icon: ImageResource.Local? = null,
    iconOnClick: () -> Unit = {},
    actionTitle: String? = null,
    actionOnClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppTheme.typography.body2,
            color = AppTheme.colors.body
        )

        icon?.let {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
            Image(
                modifier = Modifier.clickableNoEffect(onClick = iconOnClick),
                imageResource = icon.withSize(AppTheme.dimensions.smallSpacing)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        safeLet(actionTitle, actionOnClick) { text, onClick ->
            Text(
                modifier = Modifier.clickableNoEffect(onClick),
                text = text,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.primary
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
        icon = Icons.Filled.Fire
            .withTint(AppTheme.colors.warningMuted),
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
