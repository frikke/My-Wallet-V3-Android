package com.blockchain.componentlib.button.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.theme.AppTheme

@Composable
internal fun Button(
    modifier: Modifier = Modifier,
    state: ButtonState,
    shape: Shape = AppTheme.shapes.extraLarge,
    backgroundColor: Color,
    disabledBackgroundColor: Color,
    contentPadding: PaddingValues,
    onClick: () -> Unit,
    buttonContent: @Composable RowScope.() -> Unit,
) {
    androidx.compose.material.Button(
        onClick = onClick,
        modifier = modifier,
        enabled = state != ButtonState.Disabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = animateColorAsState(targetValue = backgroundColor).value,
            contentColor = Color.Unspecified,
            disabledBackgroundColor = animateColorAsState(targetValue = disabledBackgroundColor).value,
            disabledContentColor = Color.Unspecified
        ),
        contentPadding = contentPadding,
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        content = {
            buttonContent()
        }
    )
}
