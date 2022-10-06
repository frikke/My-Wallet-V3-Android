package com.blockchain.componentlib.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark300
import com.blockchain.componentlib.theme.Grey100

@Composable
fun SheetNub(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    Box(
        modifier = modifier
            .size(
                width = dimensionResource(R.dimen.standard_spacing),
                height = dimensionResource(R.dimen.smallest_spacing)
            )
            .background(
                color = if (isDarkMode) Dark300 else Grey100,
                shape = RoundedCornerShape(size = dimensionResource(R.dimen.smallest_spacing))
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
