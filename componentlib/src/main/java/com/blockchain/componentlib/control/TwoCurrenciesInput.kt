package com.blockchain.componentlib.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.chrome.BALANCE_OFFSET_ANIM_DURATION
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.UnfoldMore
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect

data class CurrencyValue(
    val value: String,
    val ticker: String,
    val isPrefix: Boolean,
    val separateWithSpace: Boolean
)

enum class InputCurrency {
    Currency1, Currency2
}

private fun InputCurrency.flip() = when (this) {
    InputCurrency.Currency1 -> InputCurrency.Currency2
    InputCurrency.Currency2 -> InputCurrency.Currency1
}

@Composable
fun TwoCurrenciesInput(
    currency1: CurrencyValue,
    onCurrency1ValueChange: (String) -> Unit,
    currency2: CurrencyValue,
    onCurrency2ValueChange: (String) -> Unit
) {
    var selected: InputCurrency by remember {
        mutableStateOf(InputCurrency.Currency1)
    }

    val localDensity = LocalDensity.current

    var textFieldHeight by remember { mutableStateOf(0) }

    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }

    DisposableEffect(selected) {
        when (selected) {
            InputCurrency.Currency1 -> focusRequester1
            InputCurrency.Currency2 -> focusRequester2
        }.requestFocus()
        onDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .then(
                if (textFieldHeight > 0) {
                    Modifier.height(
                        with(localDensity) {
                            // bottom one is scaled to 0.5 so full size should be 1.5x
                            (textFieldHeight * 1.5F).toDp()
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {

        CurrencyInput(
            modifier = Modifier
                .onGloballyPositioned {
                    if (textFieldHeight == 0) {
                        textFieldHeight = it.size.height
                    }
                },
            focused = selected == InputCurrency.Currency1,
            focusRequester = focusRequester1,
            currency = currency1,
            onCurrencyValueChange = onCurrency1ValueChange
        )

        CurrencyInput(
            focused = selected == InputCurrency.Currency2,
            focusRequester = focusRequester2,
            currency = currency2,
            onCurrencyValueChange = onCurrency2ValueChange
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
                modifier = Modifier
                    .clickableNoEffect {
                        selected = selected.flip()
                    },
                imageResource = Icons.run {
                    UnfoldMore.withBackground(
                        backgroundColor = AppTheme.colors.background,
                        iconSize = 24.dp,
                        backgroundSize = 24.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BoxScope.CurrencyInput(
    modifier: Modifier = Modifier,
    focused: Boolean,
    focusRequester: FocusRequester,
    currency: CurrencyValue,
    onCurrencyValueChange: (String) -> Unit,
) {
    var height by remember { mutableStateOf(0) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (focused) 1F else 0.5F,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    val translation by animateIntAsState(
        // scaled to 0.5 makes it half size and centered in it's 100% space
        // we want to make it sit right below the top field
        // i.e. cover the empty 25% space on top
        // -> basically move it down to <height> and up 25% == 75% of height
        targetValue = if (focused) 0 else (height * 0.75).toInt(),
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

    val interactionSource1 = remember { MutableInteractionSource() }
    BasicTextField(
        modifier = modifier
            .onGloballyPositioned {
                if (height == 0) {
                    height = it.size.height
                }
            }
            .graphicsLayer {
                translationY = translation.toFloat()
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .align(Alignment.TopCenter)
            .focusRequester(focusRequester)
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        value = currency.value,
        onValueChange = onCurrencyValueChange,
        interactionSource = interactionSource1,
        singleLine = true,
        textStyle = AppTheme.typography.display.copy(
            textAlign = TextAlign.Center,
            color = color
        ),
        readOnly = !focused,
        visualTransformation = PrefixSuffixTransformation(
            value = currency.ticker,
            isPrefix = currency.isPrefix,
            separateWithSpace = currency.separateWithSpace
        ),
    ) { innerTextField ->
        TextFieldDefaults.TextFieldDecorationBox(
            value = currency.value,
            innerTextField = innerTextField,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource1,
            contentPadding = PaddingValues(0.dp),
            enabled = true,
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
    val out = prefixWithSeparator + text.text
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

    return TransformedText(AnnotatedString(out), offsetTranslator)
}

private fun suffixFilter(
    text: AnnotatedString,
    suffix: String,
    separateWithSpace: Boolean
): TransformedText {
    val suffixWithSeparator = separator(separateWithSpace) + suffix
    val out = text.text + suffixWithSeparator
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

    return TransformedText(AnnotatedString(out), offsetTranslator)
}

// run preview on emulator to see the right layout and animations
@Preview(backgroundColor = 0x124562, showBackground = true)
@Composable
private fun PreviewTwoCurrenciesInput() {
    var c1Value by remember { mutableStateOf("2,100.00") }
    var c2Value by remember { mutableStateOf("1.1292") }

    TwoCurrenciesInput(
        currency1 = CurrencyValue(
            value = c1Value, ticker = "$", isPrefix = true, separateWithSpace = false
        ),
        onCurrency1ValueChange = { c1Value = it },

        currency2 = CurrencyValue(
            value = c2Value, ticker = "ETH", isPrefix = false, separateWithSpace = true
        ),
        onCurrency2ValueChange = { c2Value = it }
    )
}
