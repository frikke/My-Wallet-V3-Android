package com.blockchain.componentlib.button

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SplitButtons(
    primaryButtonText: String,
    primaryButtonOnClick: () -> Unit,
    secondaryButtonText: String,
    secondaryButtonOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryButtonState: ButtonState = ButtonState.Enabled,
    secondaryButtonState: ButtonState = ButtonState.Enabled,
    primaryButtonIcon: ImageResource = ImageResource.None,
    secondaryButtonIcon: ImageResource = ImageResource.None,
    primaryButtonAlignment: Alignment = Alignment.START
) {
    Row(modifier) {
        when (primaryButtonAlignment) {
            Alignment.END -> {
                SecondaryButton(
                    text = secondaryButtonText,
                    onClick = secondaryButtonOnClick,
                    state = secondaryButtonState,
                    modifier = Modifier.weight(1f),
                    icon = secondaryButtonIcon
                )
                Spacer(modifier = Modifier.width(8.dp))
                PrimaryButton(
                    text = primaryButtonText,
                    onClick = primaryButtonOnClick,
                    state = primaryButtonState,
                    modifier = Modifier.weight(1f),
                    icon = primaryButtonIcon
                )
            }
            Alignment.START -> {
                PrimaryButton(
                    text = primaryButtonText,
                    onClick = primaryButtonOnClick,
                    state = primaryButtonState,
                    modifier = Modifier.weight(1f),
                    icon = primaryButtonIcon
                )
                Spacer(modifier = Modifier.width(8.dp))
                SecondaryButton(
                    text = secondaryButtonText,
                    onClick = secondaryButtonOnClick,
                    state = secondaryButtonState,
                    modifier = Modifier.weight(1f),
                    icon = secondaryButtonIcon
                )
            }
        }
    }
}

@Preview
@Composable
private fun SplitButtonPreview() {
    AppTheme {
        AppSurface {
            SplitButtons(
                primaryButtonText = "Primary",
                primaryButtonOnClick = { },
                secondaryButtonText = "Secondary",
                secondaryButtonOnClick = { }
            )
        }
    }
}
