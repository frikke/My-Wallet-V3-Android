package com.blockchain.componentlib.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.image.Image
import com.blockchain.componentlib.image.ImageResource

@Composable
internal fun SheetHeaderBackButton(
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
    backPressContentDescription: String? = null,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    Image(
        imageResource = ImageResource.Local(
            id = if (isDarkMode) R.drawable.ic_back_chevron_dark else R.drawable.ic_back_chevron_light,
            contentDescription = backPressContentDescription,
        ),
        modifier = modifier.clickable { onBackPress() },
        contentScale = ContentScale.None,
    )
}