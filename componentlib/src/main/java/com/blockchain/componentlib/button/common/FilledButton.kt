package com.blockchain.componentlib.button.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState

@Composable
internal fun FilledButton(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color,
    backgroundColor: Color,
    disabledBackgroundColor: Color,
    contentPadding: PaddingValues,
    state: ButtonState = ButtonState.Enabled,
    style: ButtonStyle,
    icon: ImageResource.Local? = null,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        state = state,
        backgroundColor = backgroundColor,
        disabledBackgroundColor = disabledBackgroundColor,
        contentPadding = contentPadding,
        onClick = onClick,
        buttonContent = {
            DefaultButtonContent(
                state = state,
                style = style,
                text = text,
                textColor = textColor,
                icon = icon
            )
        }
    )
}
