package com.blockchain.componentlib.keyboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icons.Backspace
import com.blockchain.componentlib.icons.Fingerprint
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppTheme
import java.text.DecimalFormatSymbols
import java.util.Locale

sealed interface KeyboardButton {
    data class Value(val value: String) : KeyboardButton
    object Backspace : KeyboardButton
    object Biometrics : KeyboardButton
    object None : KeyboardButton
}

private sealed interface KeyboardType {
    object Numeric : KeyboardType
    data class Pin(val withBiometrics: Boolean) : KeyboardType
}

private fun KeyboardType.specialButton(): KeyboardButton {
    return when (this) {
        KeyboardType.Numeric -> KeyboardButton.Value(
            DecimalFormatSymbols(Locale.getDefault()).decimalSeparator.toString()
        )

        is KeyboardType.Pin -> {
            if (withBiometrics) KeyboardButton.Biometrics
            else KeyboardButton.None
        }
    }
}

@Composable
fun PinKeyboard(
    withBiometrics: Boolean,
    backgroundColor: Color = AppTheme.colors.background,
    onClick: (KeyboardButton) -> Unit
) {
    Keyboard(
        backgroundColor = backgroundColor,
        type = KeyboardType.Pin(withBiometrics),
        onClick = onClick
    )
}

@Composable
fun NumericKeyboard(
    backgroundColor: Color = AppTheme.colors.background,
    onClick: (KeyboardButton) -> Unit
) {
    Keyboard(
        backgroundColor = backgroundColor,
        type = KeyboardType.Numeric,
        onClick = onClick
    )
}

@Composable
private fun Keyboard(
    backgroundColor: Color = AppTheme.colors.background,
    type: KeyboardType,
    onClick: (KeyboardButton) -> Unit
) {
    val keyboard = listOf(
        listOf(KeyboardButton.Value("1"), KeyboardButton.Value("2"), KeyboardButton.Value("3")),
        listOf(KeyboardButton.Value("4"), KeyboardButton.Value("5"), KeyboardButton.Value("6")),
        listOf(KeyboardButton.Value("7"), KeyboardButton.Value("8"), KeyboardButton.Value("9")),
        listOf(type.specialButton(), KeyboardButton.Value("0"), KeyboardButton.Backspace)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keyboard.forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                verticalAlignment = CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { button ->
                    Surface(
                        modifier = Modifier
                            .weight(1F)
                            .fillMaxHeight(),
                        color = Color.Transparent,
                        shape = AppTheme.shapes.small
                    ) {
                        Box(
                            modifier = Modifier.clickable { onClick(button) }
                        ) {
                            KeyboardButton(
                                modifier = Modifier
                                    .padding(AppTheme.dimensions.tinySpacing)
                                    .align(Alignment.Center),
                                button = button
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardButton(
    modifier: Modifier,
    button: KeyboardButton
) {
    when (button) {
        is KeyboardButton.Value -> KeyboardNumberButton(modifier, button.value)
        KeyboardButton.Backspace -> KeyboardIconButton(modifier, Icons.Backspace)
        KeyboardButton.Biometrics -> KeyboardIconButton(modifier, Icons.Fingerprint)
        KeyboardButton.None -> {
            /*No-op*/
        }
    }
}

@Composable
private fun KeyboardNumberButton(
    modifier: Modifier,
    value: String
) {
    SimpleText(
        modifier = modifier,
        text = value,
        style = ComposeTypographies.Title2,
        color = ComposeColors.Title,
        gravity = ComposeGravities.Centre
    )
}

@Composable
private fun KeyboardIconButton(
    modifier: Modifier,
    imageResource: ImageResource.Local
) {
    Image(
        modifier = modifier,
        imageResource = imageResource.withTint(AppTheme.colors.title)
    )
}

@Preview
@Composable
private fun PreviewPinKeyboard() {
    PinKeyboard(
        withBiometrics = true,
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPinKeyboardDark() {
    PreviewPinKeyboard()
}

@Preview
@Composable
private fun PreviewNumericKeyboard() {
    NumericKeyboard(
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNumericKeyboardDark() {
    PreviewNumericKeyboard()
}
