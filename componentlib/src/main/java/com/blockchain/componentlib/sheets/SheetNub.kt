package com.blockchain.componentlib.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SheetNub(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(
                width = AppTheme.dimensions.standardSpacing,
                height = AppTheme.dimensions.smallestSpacing
            )
            .background(
                color = AppColors.dark,
                shape = RoundedCornerShape(
                    size = AppTheme.dimensions.smallestSpacing
                )
            )
    )
}

@Preview
@Composable
private fun SheetNubPreview() {
    AppTheme {
        AppSurface {
            SheetNub()
        }
    }
}
