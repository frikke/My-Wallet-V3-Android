package com.blockchain.componentlib.control

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Slider(
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    value: Float = 0f,
    enabled: Boolean = true,
) {
    androidx.compose.material.Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .padding(horizontal = dimensionResource(R.dimen.medium_spacing)),
        enabled = enabled,
        steps = 0,
        colors = SliderDefaults.colors(
            thumbColor = AppTheme.colors.primary,
            disabledThumbColor = AppTheme.colors.primary.copy(alpha = ContentAlpha.disabled),
            activeTrackColor = AppTheme.colors.primary,
            disabledActiveTrackColor = AppTheme.colors.primary.copy(alpha = SliderDefaults.DisabledActiveTrackAlpha),
        )
    )
}

@Preview
@Composable
private fun SliderPreview() {
    var value by remember { mutableStateOf(0f) }
    AppTheme {
        AppSurface {
            Slider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
            )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SliderPreviewDark() {
    var value by remember { mutableStateOf(0f) }
    AppTheme {
        AppSurface {
            Slider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
            )
        }
    }
}

@Preview
@Composable
private fun SliderPreviewDisabled() {
    var value by remember { mutableStateOf(0f) }
    AppTheme {
        AppSurface {
            Slider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
    }
}
