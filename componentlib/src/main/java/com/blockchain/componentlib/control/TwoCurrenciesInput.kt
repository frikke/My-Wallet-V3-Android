package com.blockchain.componentlib.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    val isPrefix: Boolean
)

enum class InputCurrency {
    Currency1, Currency2
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TwoCurrenciesInput(
    currency1: CurrencyValue,
    onCurrency1ValueChange: (String) -> Unit,
    currency2: CurrencyValue,
    onCurrency2ValueChange: (String) -> Unit,
    selected: InputCurrency,
    onSwitch: () -> Unit
) {
    var fullHeight by remember { mutableStateOf(0) }
    val localDensity = LocalDensity.current

    val scaleAnim1 by animateFloatAsState(
        targetValue = if (selected == InputCurrency.Currency1) 1F else 0.5F,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )
    val scaleAnim2 by animateFloatAsState(
        targetValue = if (selected == InputCurrency.Currency2) 1F else 0.5F,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    val translation1 by animateIntAsState(
        targetValue = if (selected == InputCurrency.Currency1) 0 else (fullHeight * 0.75).toInt(),
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )
    val translation2 by animateIntAsState(
        targetValue = if (selected == InputCurrency.Currency2) 0 else (fullHeight * 0.75).toInt(),
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    val color1 by animateColorAsState(
        targetValue = if (selected == InputCurrency.Currency1) AppTheme.colors.title else AppTheme.colors.body,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )
    val color2 by animateColorAsState(
        targetValue = if (selected == InputCurrency.Currency2) AppTheme.colors.title else AppTheme.colors.body,
        animationSpec = tween(
            durationMillis = BALANCE_OFFSET_ANIM_DURATION
        )
    )

    val focusRequester1 = remember { FocusRequester() }
    var isFocused1 = remember { false }

    val focusRequester2 = remember { FocusRequester() }
    var isFocused2 = remember { false }

    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .then(
                if (fullHeight > 0) {
                    Modifier.height(
                        with(localDensity) {
                            (fullHeight * 1.5F).toDp()
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {

        val interactionSource1 = remember { MutableInteractionSource() }
        BasicTextField(
            modifier = Modifier
                .onGloballyPositioned {
                    if (fullHeight == 0) {
                        fullHeight = it.size.height
                    }
                }
                .graphicsLayer {
                    translationY = translation1.toFloat()
                    scaleX = scaleAnim1
                    scaleY = scaleAnim1
                }
                .onFocusChanged {
                    isFocused1 = it.isFocused
                }
                .align(Alignment.TopCenter)
                .focusRequester(focusRequester1)
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            value = currency1.value,
            onValueChange = onCurrency1ValueChange,
            interactionSource = interactionSource1,
            singleLine = true,
            textStyle = AppTheme.typography.display.copy(textAlign = TextAlign.Center),
            readOnly = selected != InputCurrency.Currency1,
            visualTransformation = PrefixTransformation(currency1.ticker, currency1.isPrefix)
        ) { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = currency1.value,
                innerTextField = innerTextField,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource1,
                contentPadding = PaddingValues(0.dp),
                enabled = true,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = color1,
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
            )
        }

        val interactionSource2 = remember { MutableInteractionSource() }
        BasicTextField(
            modifier = Modifier
                .graphicsLayer {
                    translationY = translation2.toFloat()
                    scaleX = scaleAnim2
                    scaleY = scaleAnim2
                }
                .onFocusChanged {
                    isFocused2 = it.isFocused
                }
                .align(Alignment.TopCenter)
                .focusRequester(focusRequester2)
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            value = currency2.value,
            onValueChange = onCurrency2ValueChange,
            interactionSource = interactionSource2,
            singleLine = true,
            readOnly = selected != InputCurrency.Currency2,
            textStyle = AppTheme.typography.display.copy(textAlign = TextAlign.Center),
            visualTransformation = PrefixTransformation(currency2.ticker, currency2.isPrefix),
        ) { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = currency2.value,
                innerTextField = innerTextField,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource2,
                contentPadding = PaddingValues(0.dp),
                enabled = true,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = color2,
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
            )
        }

        var switchHeight by remember { mutableStateOf(0) }
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    if (switchHeight == 0) {
                        switchHeight = it.size.height
                    }
                }
                .graphicsLayer {
                    if (switchHeight > 0 && fullHeight > 0) {
                        translationY = ((fullHeight - switchHeight) / 2).toFloat()
                    }
                }
                .align(Alignment.TopEnd)
        ) {
            Image(
                modifier = Modifier
                    .clickableNoEffect {
                        if (selected == InputCurrency.Currency1) {
                            if (isFocused1) {
                                focusRequester1.freeFocus()
                                focusRequester2.requestFocus()
                                keyboardController?.show()
                            }
                        } else {
                            if (isFocused2) {
                                focusRequester1.requestFocus()
                                focusRequester2.freeFocus()
                                keyboardController?.show()
                            }
                        }
                        onSwitch()
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

private class PrefixTransformation(val value: String, val isPrefix: Boolean) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return if (isPrefix) {
            prefixFilter(text, value)
        } else {
            suffixFilter(text, value)
        }
    }
}

private fun prefixFilter(text: AnnotatedString, prefix: String): TransformedText {
    val out = prefix + text.text
    val prefixOffset = prefix.length

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

private fun suffixFilter(text: AnnotatedString, suffix: String): TransformedText {
    val out = text.text + suffix
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
    var top by remember { mutableStateOf(InputCurrency.Currency1) }

    var c1Value by remember { mutableStateOf("1234567") }
    var c2Value by remember { mutableStateOf("1234567") }

    TwoCurrenciesInput(
        currency1 = CurrencyValue(
            value = c1Value, ticker = "$", isPrefix = true
        ),
        onCurrency1ValueChange = { c1Value = it },

        currency2 = CurrencyValue(
            value = c2Value, ticker = "ETH", isPrefix = false
        ),
        onCurrency2ValueChange = { c2Value = it },

        selected = top,

        onSwitch = {
            top = if (top == InputCurrency.Currency1) InputCurrency.Currency2
            else InputCurrency.Currency1
        }
    )
}
