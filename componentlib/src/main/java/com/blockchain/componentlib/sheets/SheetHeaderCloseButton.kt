package com.blockchain.componentlib.sheets

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Close
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppTheme

@Composable
internal fun SheetHeaderCloseButton(
    onClosePress: () -> Unit,
    modifier: Modifier = Modifier,
    backPressContentDescription: String? = null,
    isDarkMode: Boolean = isSystemInDarkTheme()
) {
    Box(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = false),
            onClick = { onClosePress() }
        )
    ){
        Image(
            imageResource = Icons.Close
                .withContentDescription(backPressContentDescription)
                .withTint(AppTheme.colors.muted)
                .withBackground(
                    backgroundColor = AppTheme.colors.light,
                    backgroundSize = AppTheme.dimensions.standardSpacing
                ),
            contentScale = ContentScale.None,
        )
    }
}

@Preview
@Composable
private fun PreviewSheetHeaderCloseButton() {
    SheetHeaderCloseButton(
        onClosePress = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSheetHeaderCloseButtonDark() {
    PreviewSheetHeaderCloseButton()
}