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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
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
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.regex.Pattern

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
    onCurrency1ValueChange: (String) -> Unit,
    currency2: CurrencyValue,
    onCurrency2ValueChange: (String) -> Unit,
    onFlipInputs: () -> Unit
) {
    val localDensity = LocalDensity.current

    var textFieldHeight by remember { mutableStateOf(0) }

    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }

    var c1Value by remember { mutableStateOf(TextFieldValue("")) }
    LaunchedEffect(currency1.value) {
        c1Value = c1Value.copy(text = currency1.value)
    }

    var c2Value by remember { mutableStateOf(TextFieldValue("")) }
    LaunchedEffect(currency2.value) {
        c2Value = c2Value.copy(text = currency2.value)
    }

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
            maxHeight = textFieldHeight,
            focused = selected == InputCurrency.Currency1,
            focusRequester = focusRequester1,
            currency = currency1,
            value = c1Value,
            onCurrencyValueChange = {
                onCurrency1ValueChange(it.text)
                c1Value = it
            }
        )

        CurrencyInput(
            maxHeight = textFieldHeight,
            focused = selected == InputCurrency.Currency2,
            focusRequester = focusRequester2,
            currency = currency2,
            value = c2Value,
            onCurrencyValueChange = {
                onCurrency2ValueChange(it.text)
                c2Value = it
            }
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

                    when (selected.flip()) {
                        InputCurrency.Currency1 -> {
                            c1Value = c1Value.copy(selection = TextRange(0, c1Value.text.length))
                        }
                        InputCurrency.Currency2 -> {
                            c2Value = c2Value.copy(selection = TextRange(0, c2Value.text.length))
                        }
                    }
                },
                imageResource = Icons.UnfoldMore.withBackground(
                    backgroundColor = AppTheme.colors.background,
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
    focusRequester: FocusRequester,
    currency: CurrencyValue,
    value: TextFieldValue,
    onCurrencyValueChange: (TextFieldValue) -> Unit
) {
    val pattern = remember(currency.maxFractionDigits) {
        val decimalSeparator = DecimalFormatSymbols(Locale.getDefault()).decimalSeparator.toString()
        Pattern.compile(
            "-?\\d{0,100}+((\\$decimalSeparator\\d{0,${currency.maxFractionDigits}})?)||(\\$decimalSeparator)?"
        )
    }

    val textSizeAnim by animateFloatAsState(
        targetValue = if (focused) 40F else 20F,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    val translation by animateIntAsState(
        targetValue = if (focused) 0 else maxHeight,
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

    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        modifier = modifier
            .graphicsLayer {
                translationY = translation.toFloat()
            }
            .align(Alignment.TopCenter)
            .focusRequester(focusRequester)
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        value = value,
        onValueChange = {
            if (pattern.matcher(it.text).matches()) {
                onCurrencyValueChange(it)
            }
        },
        interactionSource = interactionSource,
        singleLine = true,
        textStyle = AppTheme.typography.display.copy(
            fontSize = textSizeAnim.sp,
            textAlign = TextAlign.Center,
            color = color
        ),
        readOnly = !focused,
        visualTransformation = PrefixSuffixTransformation(
            value = currency.ticker,
            isPrefix = currency.isPrefix,
            separateWithSpace = currency.separateWithSpace
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    ) { innerTextField ->
        TextFieldDefaults.TextFieldDecorationBox(
            value = value.text,
            innerTextField = innerTextField,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(0.dp),
            enabled = true,
            placeholder = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = currency.zeroHint(),
                    style = AppTheme.typography.display.copy(
                        fontSize = textSizeAnim.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

        )
    }
}

@Composable
private fun CurrencyValue.zeroHint(): AnnotatedString {
    @Composable
    fun color(visible: Boolean): Color {
        return if (visible) AppTheme.colors.dark else AppTheme.colors.backgroundMuted
    }

    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = color(!isPrefix))) {
            append(zeroHint + separator(separateWithSpace && !isPrefix))
        }

        withStyle(style = SpanStyle(color = AppTheme.colors.title)) {
            append(ticker)
        }

        withStyle(style = SpanStyle(color = color(isPrefix))) {
            append(separator(separateWithSpace && isPrefix) + zeroHint)
        }
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
@Preview(backgroundColor = 0XFFF1F2F7, showBackground = true)
@Composable
private fun PreviewTwoCurrenciesInput() {
    var c1Value by remember { mutableStateOf("2100.00") }
    var c2Value by remember { mutableStateOf("1.1292") }

    var selected: InputCurrency by remember {
        mutableStateOf(InputCurrency.Currency1)
    }

    TwoCurrenciesInput(
        selected = selected,
        currency1 = CurrencyValue(
            value = c1Value,
            maxFractionDigits = 2,
            ticker = "$",
            isPrefix = true,
            separateWithSpace = false,
            zeroHint = "0"
        ),
        onCurrency1ValueChange = { c1Value = it },
        currency2 = CurrencyValue(
            value = c2Value,
            maxFractionDigits = 8,
            ticker = "ETH",
            isPrefix = false,
            separateWithSpace = true,
            zeroHint = "0"
        ),
        onCurrency2ValueChange = { c2Value = it },
        onFlipInputs = {
            selected = selected.flip()
        }
    )
}
