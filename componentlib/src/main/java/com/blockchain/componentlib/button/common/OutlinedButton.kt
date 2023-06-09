package com.blockchain.componentlib.button.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.SnackbarDefaults.backgroundColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.ImageResource.None.shape
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.theme.AppTheme

@Composable
internal fun OutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color,
    backgroundColor: Color,
    disabledBackgroundColor: Color,
    borderColor: Color,
    state: ButtonState = ButtonState.Enabled,
    style: ButtonStyle,
    icon: ImageResource.Local? = null,
    customIconTint: Color? = null,
    onClick: () -> Unit,
) {
    androidx.compose.material.OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = state != ButtonState.Disabled,
        shape = AppTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = animateColorAsState(targetValue = backgroundColor).value,
            contentColor = Color.Unspecified,
            disabledBackgroundColor = animateColorAsState(targetValue = disabledBackgroundColor).value,
            disabledContentColor = Color.Unspecified
        ),
        border = BorderStroke(
            width = 1.dp,
            color = animateColorAsState(targetValue = borderColor).value
        ),
        contentPadding = style.contentPadding,
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        content = {
            DefaultButtonContent(
                state = state,
                style = style,
                text = text,
                textColor = textColor,
                icon = icon,
                customIconTint = customIconTint
            )
        }
    )
}
