package com.blockchain.componentlib.sectionheader

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
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
fun SmallSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(color = AppTheme.colors.light),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = AppTheme.typography.caption2,
            color = AppTheme.colors.title,
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.standardSpacing,
                vertical = AppTheme.dimensions.tinySpacing,
            )
        )
    }
}

@Preview
@Composable
private fun SmallSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            SmallSectionHeader("Title", Modifier.fillMaxWidth())
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SmallSectionHeaderDarkPreview() {
    AppTheme {
        AppSurface {
            SmallSectionHeader("Title", Modifier.fillMaxWidth())
        }
    }
}
