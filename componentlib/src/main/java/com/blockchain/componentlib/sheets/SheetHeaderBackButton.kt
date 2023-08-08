package com.blockchain.componentlib.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource

@Composable
internal fun SheetHeaderBackButton(
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
    backPressContentDescription: String? = null,
    isDarkMode: Boolean = isSystemInDarkTheme()
) {
    Image(
        imageResource = ImageResource.Local(
            id = if (isDarkMode) R.drawable.ic_back_chevron_dark else R.drawable.ic_back_chevron_light,
            contentDescription = backPressContentDescription
        ),
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = false),
            onClick = { onBackPress() }
        ),
        contentScale = ContentScale.None
    )
}
