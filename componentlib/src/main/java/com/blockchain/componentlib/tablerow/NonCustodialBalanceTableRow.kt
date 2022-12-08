package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun NonCustodialAssetBalanceTableRow(
    title: String,
    valueCrypto: String,
    valueFiat: String,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    TableRow(
        contentStart = contentStart,
        onContentClicked = onClick,
        content = {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )

                Spacer(modifier = Modifier.weight(1F))
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = valueFiat,
                        style = AppTheme.typography.paragraph2,
                        color = AppTheme.colors.title
                    )
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                    Text(
                        text = valueCrypto,
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun NonCustodialBalanceTableRowPreview() {
    AppTheme {
        NonCustodialAssetBalanceTableRow(title = "Bitcoin", valueCrypto = "1", valueFiat = "1232222") {}
    }
}
