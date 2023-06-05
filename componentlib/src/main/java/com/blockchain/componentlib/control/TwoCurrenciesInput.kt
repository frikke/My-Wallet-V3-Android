package com.blockchain.componentlib.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.chrome.BALANCE_OFFSET_ANIM_DURATION
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.UnfoldMore
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.componentlib.utils.conditional

data class CurrencyValue(
    val value: String,
    val maxFractionDigits: Int,
    val ticker: String,
    val isPrefix: Boolean,
    val separateWithSpace: Boolean,
    val zeroHint: String
)

fun CurrencyValue.isEmpty() = value.isEmpty()

enum class InputCurrency {
    Currency1, Currency2
}

fun InputCurrency.flip() = when (this) {
    InputCurrency.Currency1 -> InputCurrency.Currency2
    InputCurrency.Currency2 -> InputCurrency.Currency1
}

@Composable
fun TwoCurrenciesInput(
    selected: InputCurrency,
    currency1: CurrencyValue,
    currency2: CurrencyValue,
    onFlipInputs: () -> Unit
) {
    val localDensity = LocalDensity.current

    var textFieldHeight by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .conditional(textFieldHeight > 0) {
                height(
                    with(localDensity) {
                        // bottom one is scaled to 0.5 so full size should be 1.5x
                        (textFieldHeight * 1.5F).toDp()
                    }
                )
            }
    ) {
        val inputHeightModifier = Modifier.onGloballyPositioned {
            if (textFieldHeight == 0) {
                textFieldHeight = it.size.height
            }
        }

        CurrencyInput(
            modifier = Modifier
                .conditional(selected == InputCurrency.Currency1) {
                    inputHeightModifier
                },
            maxHeight = textFieldHeight,
            focused = selected == InputCurrency.Currency1,
            currencyValue = currency1,
        )

        CurrencyInput(
            modifier = Modifier
                .conditional(selected == InputCurrency.Currency2) {
                    inputHeightModifier
                },
            maxHeight = textFieldHeight,
            focused = selected == InputCurrency.Currency2,
            currencyValue = currency2,
        )

        var switchHeight by remember { mutableStateOf(0) }
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    if (switchHeight == 0) {
                        switchHeight = it.size.height
                    }
                }
                .graphicsLayer {
                    if (switchHeight > 0 && textFieldHeight > 0) {
                        translationY = ((textFieldHeight - switchHeight) / 2).toFloat()
                    }
                }
                .align(Alignment.TopEnd)
        ) {
            Image(
                modifier = Modifier.clickableNoEffect {
                    onFlipInputs()
                },
                imageResource = Icons.UnfoldMore.withBackground(
                    backgroundColor = AppTheme.colors.backgroundSecondary,
                    iconSize = 24.dp,
                    backgroundSize = 24.dp,
                    shape = RoundedCornerShape(8.dp)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BoxScope.CurrencyInput(
    modifier: Modifier = Modifier,
    maxHeight: Int,
    focused: Boolean,
    currencyValue: CurrencyValue,
) {
    val prefixSuffixTransformation = PrefixSuffixTransformation(
        value = currencyValue.ticker,
        isPrefix = currencyValue.isPrefix,
        separateWithSpace = currencyValue.separateWithSpace
    )

    val rawValue = AnnotatedString(currencyValue.value).ifEmpty {
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = AppTheme.colors.muted)) {
                append(currencyValue.zeroHint)
            }
        }
    }
    val value = prefixSuffixTransformation.filter(rawValue).text

    var textFieldHeight by remember { mutableStateOf(0) }

    val textSizeAnim by animateFloatAsState(
        targetValue = if (focused) 40F else 20F,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    val translation by animateIntAsState(
        targetValue = if (focused) (maxHeight - textFieldHeight) / 2 else maxHeight,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    val color by animateColorAsState(
        targetValue = if (focused) AppTheme.colors.title else AppTheme.colors.body,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    BoxWithConstraints(modifier.fillMaxWidth(1f)) {
        val horizontalPadding = AppTheme.dimensions.hugeSpacing
        val textStyle = AppTheme.typography.display

        val density = LocalDensity.current
        val context = LocalContext.current

        val maxFontSize = remember(maxWidth, value) {
            var shrunkFontSize = textStyle.fontSize
            val calculateIntrinsics = {
                ParagraphIntrinsics(
                    text = value.text,
                    style = textStyle.copy(fontSize = shrunkFontSize),
                    density = density,
                    fontFamilyResolver = createFontFamilyResolver(context)
                )
            }

            var intrinsics = calculateIntrinsics()
            with(density) {
                val textFieldDefaultHorizontalPadding = horizontalPadding.toPx()
                val maxInputWidth = maxWidth.toPx() - 2 * textFieldDefaultHorizontalPadding

                while (intrinsics.maxIntrinsicWidth > maxInputWidth) {
                    shrunkFontSize *= 0.9
                    intrinsics = calculateIntrinsics()
                }
            }
            shrunkFontSize
        }
        val fontSize = textSizeAnim.coerceAtMost(maxFontSize.value).sp

        Text(
            modifier = modifier
                .conditional(focused) {
                    onGloballyPositioned {
                        textFieldHeight = it.size.height
                    }
                }
                .graphicsLayer {
                    translationY = translation.toFloat()
                }
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            text = value,
            style = textStyle.copy(
                fontSize = fontSize,
                color = color
            ),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private class PrefixSuffixTransformation(
    val value: String,
    val isPrefix: Boolean,
    val separateWithSpace: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return if (isPrefix) {
            prefixFilter(text = text, prefix = value, separateWithSpace = separateWithSpace)
        } else {
            suffixFilter(text = text, suffix = value, separateWithSpace = separateWithSpace)
        }
    }
}

private fun separator(shouldExist: Boolean) = " ".takeIf { shouldExist } ?: ""

private fun prefixFilter(
    text: AnnotatedString,
    prefix: String,
    separateWithSpace: Boolean
): TransformedText {
    val prefixWithSeparator = prefix + separator(separateWithSpace)
    val out = AnnotatedString(prefixWithSeparator) + text
    val prefixOffset = prefixWithSeparator.length

    val offsetTranslator = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return offset + prefixOffset
        }

        override fun transformedToOriginal(offset: Int): Int {
            if (offset < prefixOffset) return 0
            return offset - prefixOffset
        }
    }

    return TransformedText(out, offsetTranslator)
}

private fun suffixFilter(
    text: AnnotatedString,
    suffix: String,
    separateWithSpace: Boolean
): TransformedText {
    val suffixWithSeparator = separator(separateWithSpace) + suffix
    val out = text + AnnotatedString(suffixWithSeparator)
    val suffixOffset = text.length

    val offsetTranslator = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return offset
        }

        override fun transformedToOriginal(offset: Int): Int {
            if (offset > suffixOffset) return suffixOffset
            return offset
        }
    }

    return TransformedText(out, offsetTranslator)
}

// run preview on emulator to see the right layout and animations
@Preview(backgroundColor = 0XFFF1F2F7, showBackground = true)
@Composable
private fun PreviewTwoCurrenciesInput() {
    var selected: InputCurrency by remember {
        mutableStateOf(InputCurrency.Currency1)
    }

    TwoCurrenciesInput(
        selected = selected,
        currency1 = CurrencyValue(
            value = "2100.00",
            maxFractionDigits = 2,
            ticker = "$",
            isPrefix = true,
            separateWithSpace = false,
            zeroHint = "0"
        ),
        currency2 = CurrencyValue(
            value = "1.1292",
            maxFractionDigits = 8,
            ticker = "ETH",
            isPrefix = false,
            separateWithSpace = true,
            zeroHint = "0"
        ),
        onFlipInputs = {
            selected = selected.flip()
        }
    )
}
