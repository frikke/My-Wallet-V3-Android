package com.blockchain.componentlib.button

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ButtonContentSmall(
    state: ButtonState,
    text: String,
    textColor: Color,
    textAlpha: Float,
    modifier: Modifier = Modifier,
    @DrawableRes loadingIconResId: Int = R.drawable.ic_loading
) {
    Box(modifier.animateContentSize()) {
        if (state == ButtonState.Loading) {
            ButtonLoadingIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(16.dp),
                loadingIconResId = loadingIconResId,
            )
        } else {
            Text(
                text = text,
                color = textColor,
                modifier = Modifier.alpha(textAlpha),
                style = AppTheme.typography.paragraph2,
            )
        }
    }
}
