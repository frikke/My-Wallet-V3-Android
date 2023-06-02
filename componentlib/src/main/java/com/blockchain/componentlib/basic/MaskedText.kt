package com.blockchain.componentlib.basic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.mask.MaskedValueService
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.compose.get

private const val MASKED_TEXT = "••••"

enum class MaskedTextFormat {
    ClearThenMasked, MaskedThenClear
}

/**
 * only masked text
 */
@Composable
fun MaskedText(
    modifier: Modifier = Modifier,
    allowMaskedValue: Boolean = true,
    text: String,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign? = null,
) {
    MaskedText(
        modifier = modifier,
        allowMaskedValue = allowMaskedValue,
        clearText = "",
        maskableText = text,
        format = MaskedTextFormat.ClearThenMasked,
        style = style,
        color = color,
        textAlign = textAlign,
    )
}

/**
 * masked text with a portion that should remain clear, $100 - $••••
 */
@Composable
fun MaskedText(
    modifier: Modifier = Modifier,
    allowMaskedValue: Boolean = true,
    clearText: String,
    maskableText: String,
    format: MaskedTextFormat,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign? = null,
) {

    val shouldMask by if (allowMaskedValue) {
        maskedValueServiceProvider().shouldMask.collectAsStateLifecycleAware()
    } else {
        remember { mutableStateOf(false) }
    }

    Text(
        modifier = modifier,
        text = when (format) {
            MaskedTextFormat.ClearThenMasked -> clearText + (maskableText.takeIf { !shouldMask } ?: MASKED_TEXT)
            MaskedTextFormat.MaskedThenClear -> (maskableText.takeIf { !shouldMask } ?: MASKED_TEXT) + clearText
        },
        style = style,
        color = color,
        textAlign = textAlign
    )
}

@Composable
private fun maskedValueServiceProvider(): MaskedValueService {
    var maskedValueService: MaskedValueService by remember { mutableStateOf(previewMaskedValueService) }
    if (!LocalInspectionMode.current) {
        maskedValueService = get()
    }
    return maskedValueService
}

@Preview
@Composable
private fun PreviewMaskedTextMasked() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        MaskedText(
            text = "$100",
            style = AppTheme.typography.title1,
            color = AppTheme.colors.title,
        )
        PrimaryButton(text = "switch", onClick = { previewMaskedValueService.toggleMaskState() })
    }
}

@Preview
@Composable
private fun PreviewMaskedAndClearText() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        MaskedText(
            clearText = "$",
            maskableText = "100",
            format = MaskedTextFormat.ClearThenMasked,
            style = AppTheme.typography.title1,
            color = AppTheme.colors.title,
        )
        PrimaryButton(text = "switch", onClick = { previewMaskedValueService.toggleMaskState() })
    }
}

private val previewMaskedValueService = object : MaskedValueService {
    override val shouldMask = MutableStateFlow(false)

    override fun updateMaskState(shouldMask: Boolean) {
        this.shouldMask.value = shouldMask
    }

    override fun toggleMaskState() {
        this.shouldMask.value = !this.shouldMask.value
    }
}