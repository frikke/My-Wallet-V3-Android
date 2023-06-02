package com.blockchain.componentlib.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Visible
import com.blockchain.componentlib.icons.VisibleOff
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.mask.MaskedValueService
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.compose.get

private const val MASKED_TEXT = "••••"

@Stable
enum class MaskedTextFormat {
    ClearThenMasked, MaskedThenClear
}

@Stable
sealed interface MaskStateConfig {
    object Default : MaskStateConfig
    data class Override(val maskEnabled: Boolean) : MaskStateConfig
}

/**
 * only masked text
 */
@Composable
fun MaskableText(
    modifier: Modifier = Modifier,
    maskState: MaskStateConfig = MaskStateConfig.Default,
    text: String,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign? = null,
) {
    MaskableText(
        modifier = modifier,
        maskState = maskState,
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
fun MaskableText(
    modifier: Modifier = Modifier,
    maskState: MaskStateConfig = MaskStateConfig.Default,
    clearText: String,
    maskableText: String,
    format: MaskedTextFormat,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign? = null
) {

    val shouldMask by when (maskState) {
        MaskStateConfig.Default -> maskedValueServiceProvider().shouldMask.collectAsStateLifecycleAware()
        is MaskStateConfig.Override -> remember(maskState.maskEnabled) { mutableStateOf(maskState.maskEnabled) }
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
fun MaskableTextWithToggle(
    modifier: Modifier = Modifier,
    clearText: String,
    maskableText: String,
    format: MaskedTextFormat,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign? = null,
) {
    val maskedValueService = maskedValueServiceProvider()
    val isMaskActive by maskedValueService.shouldMask.collectAsStateLifecycleAware()

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1F))

        MaskableText(
            modifier = modifier,
            maskState = MaskStateConfig.Default,
            clearText = clearText,
            maskableText = maskableText,
            format = format,
            style = style,
            color = color,
            textAlign = textAlign
        )

        Row(modifier = Modifier.weight(1F)) {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

            Surface(
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Image(
                    modifier = Modifier
                        .clickable(
                            onClick = {
                                maskedValueService.toggleMaskState()
                            }
                        )
                        .padding(AppTheme.dimensions.smallestSpacing),
                    imageResource = if (isMaskActive) {
                        Icons.Filled.VisibleOff
                    } else {
                        Icons.Filled.Visible
                    }.withTint(AppTheme.colors.dark)
                )
            }
        }
    }
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
        MaskableText(
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
        MaskableText(
            clearText = "$",
            maskableText = "100",
            format = MaskedTextFormat.ClearThenMasked,
            style = AppTheme.typography.title1,
            color = AppTheme.colors.title,
        )
        PrimaryButton(text = "switch", onClick = { previewMaskedValueService.toggleMaskState() })
    }
}

@Preview
@Composable
private fun PreviewMaskableTextWithToggle() {
    MaskableTextWithToggle(
        clearText = "$",
        maskableText = "100",
        format = MaskedTextFormat.ClearThenMasked,
        style = AppTheme.typography.title1,
        color = AppTheme.colors.title,
    )
}

private val previewMaskedValueService = object : MaskedValueService {
    override val shouldMask = MutableStateFlow(false)

    override fun toggleMaskState() {
        this.shouldMask.value = !this.shouldMask.value
    }
}
