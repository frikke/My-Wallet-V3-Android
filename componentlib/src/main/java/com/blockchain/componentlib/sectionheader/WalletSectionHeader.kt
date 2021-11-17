package com.blockchain.componentlib.sectionheader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun WalletSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = AppTheme.typography.caption2,
            color = AppTheme.colors.title,
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.paddingLarge,
                vertical = AppTheme.dimensions.paddingSmall,
            )
        )
    }
}

@Preview
@Composable
private fun WalletSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            WalletSectionHeader("Title", Modifier.fillMaxWidth())
        }
    }
}
