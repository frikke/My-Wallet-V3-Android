package com.blockchain.componentlib.controls

import android.view.KeyEvent.ACTION_DOWN
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark200
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Dark700
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey600

sealed class TextInputState(val message: String? = null) {
    data class Default(val defaultMessage: String? = null) : TextInputState(defaultMessage)
    data class Success(val successMessage: String? = null) : TextInputState(successMessage)
    data class Error(val errorMessage: String? = null) : TextInputState(errorMessage)
    data class Disabled(val disabledMessage: String? = null) : TextInputState(disabledMessage)
}

@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    autoSize: Boolean = false,
    readOnly: Boolean = false,
    state: TextInputState = TextInputState.Default(""),
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageResource = ImageResource.None,
    trailingIcon: ImageResource = ImageResource.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    maxLength: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onFocusChanged: (FocusState) -> Unit = {},
    onTrailingIconClicked: () -> Unit = {}
) {
    // Holds the latest internal TextFieldValue state. We need to keep it to have the correct value
    // of the composition.
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    // Holds the latest TextFieldValue that BasicTextField was recomposed with. We couldn't simply
    // pass `TextFieldValue(text = value)` to the CoreTextField because we need to preserve the
    // composition.
    val textFieldValue = textFieldValueState.copy(text = value)
    // Last String value that either text field was recomposed with or updated in the onValueChange
    // callback. We keep track of it to prevent calling onValueChange(String) for same String when
    // CoreTextField's onValueChange is called multiple times without recomposition in between.
    var lastTextValue by remember(value) { mutableStateOf(value) }

    TextInput(
        modifier = modifier,
        value = textFieldValue,
        onValueChange = { newTextFieldValueState ->
            textFieldValueState = newTextFieldValueState

            val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
            lastTextValue = newTextFieldValueState.text

            if (stringChangedSinceLastInvocation) {
                onValueChange(newTextFieldValueState.text)
            }
        },
        autoSize = autoSize,
        readOnly = readOnly,
        state = state,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        maxLength = maxLength,
        interactionSource = interactionSource,
        onFocusChanged = onFocusChanged,
        onTrailingIconClicked = onTrailingIconClicked
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    autoSize: Boolean = false,
    readOnly: Boolean = false,
    state: TextInputState = TextInputState.Default(""),
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageResource = ImageResource.None,
    trailingIcon: ImageResource = ImageResource.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    maxLength: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onFocusChanged: (FocusState) -> Unit = {},
    onTrailingIconClicked: () -> Unit = {}
) {
    val enabled = state !is TextInputState.Disabled

    val assistiveTextColor = when (state) {
        is TextInputState.Default, is TextInputState.Disabled -> if (!isSystemInDarkTheme()) {
            Grey600
        } else {
            Color.White
        }
        is TextInputState.Error -> AppTheme.colors.error
        is TextInputState.Success -> AppTheme.colors.success
    }

    val unfocusedColor = when (state) {
        is TextInputState.Default, is TextInputState.Disabled -> if (!isSystemInDarkTheme()) {
            Grey000
        } else {
            Dark600
        }
        is TextInputState.Error -> AppTheme.colors.error
        is TextInputState.Success -> AppTheme.colors.success
    }

    val focusedColor = when (state) {
        is TextInputState.Default, is TextInputState.Disabled -> AppTheme.colors.primary
        is TextInputState.Error -> AppTheme.colors.error
        is TextInputState.Success -> AppTheme.colors.success
    }

    val textColor = if (enabled) {
        AppTheme.colors.title
    } else {
        Grey600
    }

    val backgroundColor = if (enabled) {
        AppTheme.colors.light
    } else {
        if (!isSystemInDarkTheme()) {
            Grey000
        } else {
            Dark700
        }
    }

    val placeholderColor = if (!isSystemInDarkTheme()) {
        Grey600
    } else {
        Dark200
    }

    val newTextFieldValue = if (maxLength != Int.MAX_VALUE) {
        val newValueString = value.annotatedString.subSequence(
            0,
            value.annotatedString.length.coerceAtMost(maxLength)
        )
        value.copy(annotatedString = newValueString)
    } else {
        value
    }
    Column {
        val focusManager = LocalFocusManager.current

        BoxWithConstraints(modifier.fillMaxWidth(1f)) {
            val textStyle = AppTheme.typography.body1

            val fontSize = if (autoSize) {
                check(singleLine) { "autoSize only works with singleLine = false" }

                val density = LocalDensity.current
                val context = LocalContext.current

                val fontSize = remember(maxWidth, newTextFieldValue.text) {
                    var shrunkFontSize = textStyle.fontSize
                    val calculateIntrinsics = {
                        ParagraphIntrinsics(
                            text = newTextFieldValue.text,
                            style = textStyle,
                            density = density,
                            fontFamilyResolver = createFontFamilyResolver(context)
                        )
                    }

                    var intrinsics = calculateIntrinsics()
                    with(density) {
                        // TextField and OutlinedText field have default horizontal padding of 16.dp
                        val textFieldDefaultHorizontalPadding = 16.dp.toPx()
                        val maxInputWidth = maxWidth.toPx() - 2 * textFieldDefaultHorizontalPadding

                        while (intrinsics.maxIntrinsicWidth > maxInputWidth) {
                            shrunkFontSize *= 0.9
                            intrinsics = calculateIntrinsics()
                        }
                    }
                    shrunkFontSize
                }
                fontSize
            } else {
                textStyle.fontSize
            }

            TextField(
                value = newTextFieldValue,
                onValueChange = { newValue ->
                    val newTextFieldValue = if (maxLength != Int.MAX_VALUE) {
                        val newValueString = newValue.annotatedString.subSequence(
                            0,
                            newValue.annotatedString.length.coerceAtMost(maxLength)
                        )
                        newValue.copy(annotatedString = newValueString)
                    } else {
                        newValue
                    }
                    onValueChange(newTextFieldValue)
                },
                modifier = modifier
                    .fillMaxWidth(1f)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Tab && keyEvent.nativeKeyEvent.action == ACTION_DOWN) {
                            focusManager.moveFocus(FocusDirection.Next)
                            true
                        } else {
                            false
                        }
                    }
                    .onFocusChanged { focusState ->
                        onFocusChanged.invoke(focusState)
                    },
                label = if (label != null) {
                    { Text(label, style = AppTheme.typography.caption1) }
                } else {
                    null
                },
                placeholder = if (placeholder != null) {
                    { Text(placeholder) }
                } else {
                    null
                },
                leadingIcon = if (leadingIcon != ImageResource.None) {
                    {
                        Image(imageResource = leadingIcon)
                    }
                } else {
                    null
                },
                trailingIcon = if (trailingIcon != ImageResource.None) {
                    {
                        Image(
                            modifier = Modifier.clickable {
                                onTrailingIconClicked.invoke()
                            },
                            imageResource = trailingIcon
                        )
                    }
                } else {
                    null
                },
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle.copy(fontSize = fontSize),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    unfocusedLabelColor = placeholderColor,
                    unfocusedIndicatorColor = unfocusedColor,
                    focusedIndicatorColor = focusedColor,
                    focusedLabelColor = focusedColor,
                    cursorColor = focusedColor,
                    errorCursorColor = focusedColor,
                    placeholderColor = placeholderColor,
                    disabledTextColor = textColor,
                    disabledLabelColor = placeholderColor,
                    disabledPlaceholderColor = placeholderColor
                ),
                interactionSource = interactionSource
            )
        }

        if (state.message != null) {
            Text(
                text = state.message,
                color = assistiveTextColor,
                style = AppTheme.typography.caption1,
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(
                        start = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing),
                        end = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing),
                        top = 8.dp
                    )
            )
        }
    }
}

@Preview
@Composable
fun TextInput_Error_Preview() {
    AppTheme {
        AppSurface {
            TextInput(
                value = "",
                label = "Home Address",
                onValueChange = {},
                placeholder = "Placeholder text",
                trailingIcon = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_search),
                state = TextInputState.Error("Test Error Message")
            )
        }
    }
}

@Preview
@Composable
fun TextInput_Disabled_Preview() {
    AppTheme {
        AppSurface {
            TextInput(
                value = "Input",
                onValueChange = {},
                state = TextInputState.Disabled("Test Disabled message")
            )
        }
    }
}

@Preview
@Composable
fun TextInput_Success_Preview() {
    AppTheme {
        AppSurface {
            TextInput(
                value = "Input",
                onValueChange = {},
                state = TextInputState.Success("Test Disabled message")
            )
        }
    }
}

@Preview
@Composable
fun TextInput_Preview() {
    AppTheme {
        AppSurface {
            TextInput(
                value = "Input",
                onValueChange = {},
                state = TextInputState.Default()
            )
        }
    }
}

@Composable
fun OutlinedTextInput(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp),
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    state: TextInputState = TextInputState.Default(null),
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageResource = ImageResource.None,
    unfocusedTrailingIcon: ImageResource = ImageResource.None,
    focusedTrailingIcon: ImageResource = ImageResource.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    maxLength: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hasFocus: MutableState<Boolean> = remember { mutableStateOf(false) },
    onFocusChanged: (FocusState) -> Unit = {},
    onTrailingIconClicked: () -> Unit = {}
) {
    // Holds the latest internal TextFieldValue state. We need to keep it to have the correct value
    // of the composition.
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    // Holds the latest TextFieldValue that BasicTextField was recomposed with. We couldn't simply
    // pass `TextFieldValue(text = value)` to the CoreTextField because we need to preserve the
    // composition.
    val textFieldValue = textFieldValueState.copy(text = value)
    // Last String value that either text field was recomposed with or updated in the onValueChange
    // callback. We keep track of it to prevent calling onValueChange(String) for same String when
    // CoreTextField's onValueChange is called multiple times without recomposition in between.
    var lastTextValue by remember(value) { mutableStateOf(value) }

    OutlinedTextInput(
        modifier = modifier,
        shape = shape,
        value = textFieldValue,
        onValueChange = { newTextFieldValueState ->
            textFieldValueState = newTextFieldValueState

            val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
            lastTextValue = newTextFieldValueState.text

            if (stringChangedSinceLastInvocation) {
                onValueChange(newTextFieldValueState.text)
            }
        },
        readOnly = readOnly,
        state = state,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        unfocusedTrailingIcon = unfocusedTrailingIcon,
        focusedTrailingIcon = focusedTrailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        maxLength = maxLength,
        interactionSource = interactionSource,
        hasFocus = hasFocus,
        onFocusChanged = onFocusChanged,
        onTrailingIconClicked = onTrailingIconClicked
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OutlinedTextInput(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp),
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    readOnly: Boolean = false,
    state: TextInputState = TextInputState.Default(null),
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageResource = ImageResource.None,
    unfocusedTrailingIcon: ImageResource = ImageResource.None,
    focusedTrailingIcon: ImageResource = ImageResource.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    maxLength: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hasFocus: MutableState<Boolean> = remember { mutableStateOf(false) },
    onFocusChanged: (FocusState) -> Unit = {},
    onTrailingIconClicked: () -> Unit = {}
) {
    val enabled = state !is TextInputState.Disabled

    val assistiveTextColor = when (state) {
        is TextInputState.Default,
        is TextInputState.Disabled -> AppTheme.colors.body
        is TextInputState.Error -> AppTheme.colors.error
        is TextInputState.Success -> AppTheme.colors.success
    }

    val unfocusedColor = when (state) {
        is TextInputState.Default, is TextInputState.Disabled -> Color.Transparent
        is TextInputState.Error -> AppTheme.colors.error
        is TextInputState.Success -> AppTheme.colors.success
    }

    val focusedColor = when (state) {
        is TextInputState.Default, is TextInputState.Disabled -> AppTheme.colors.primary
        is TextInputState.Error -> AppTheme.colors.error
        is TextInputState.Success -> AppTheme.colors.success
    }

    val textColor = if (enabled) {
        AppTheme.colors.title
    } else {
        AppTheme.colors.body
    }

    val backgroundColor = if (enabled) {
        AppTheme.colors.backgroundSecondary
    } else {
        AppTheme.colors.medium
    }

    val placeholderColor = AppTheme.colors.body

    val borderColor = if (hasFocus.value) {
        focusedColor
    } else {
        unfocusedColor
    }

    val trailingIcon = if (hasFocus.value) {
        focusedTrailingIcon
    } else {
        unfocusedTrailingIcon
    }

    Column(modifier = modifier) {
        val focusManager = LocalFocusManager.current
        Box(
            modifier = Modifier
                .border(BorderStroke(1.dp, borderColor), shape)
                .background(color = backgroundColor, shape = shape)
        ) {
            val newTextFieldValue = if (maxLength != Int.MAX_VALUE) {
                val newValueString = value.annotatedString.subSequence(
                    0,
                    value.annotatedString.length.coerceAtMost(maxLength)
                )
                value.copy(annotatedString = newValueString)
            } else {
                value
            }
            TextField(
                value = newTextFieldValue,
                onValueChange = { newValue ->
                    val newTextFieldValue = if (maxLength != Int.MAX_VALUE) {
                        val newValueString = newValue.annotatedString.subSequence(
                            0,
                            newValue.annotatedString.length.coerceAtMost(maxLength)
                        )
                        newValue.copy(annotatedString = newValueString)
                    } else {
                        newValue
                    }
                    onValueChange(newTextFieldValue)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Tab && keyEvent.nativeKeyEvent.action == ACTION_DOWN) {
                            focusManager.moveFocus(FocusDirection.Next)
                            true
                        } else {
                            false
                        }
                    }
                    .onFocusChanged { focusState ->
                        onFocusChanged.invoke(focusState)
                        hasFocus.value = focusState.isFocused
                    },
                label = if (label != null) {
                    { Text(label, style = AppTheme.typography.caption1) }
                } else {
                    null
                },
                placeholder = if (placeholder != null) {
                    {
                        SimpleText(
                            modifier = Modifier.fillMaxWidth(),
                            text = placeholder,
                            style = ComposeTypographies.Body1,
                            gravity = ComposeGravities.Start,
                            color = ComposeColors.Muted
                        )
                    }
                } else {
                    null
                },
                leadingIcon = if (leadingIcon != ImageResource.None) {
                    {
                        Image(imageResource = leadingIcon)
                    }
                } else {
                    null
                },
                trailingIcon = if (trailingIcon != ImageResource.None) {
                    {
                        Image(
                            modifier = Modifier.clickable {
                                onTrailingIconClicked.invoke()
                            },
                            imageResource = trailingIcon
                        )
                    }
                } else {
                    null
                },
                enabled = enabled,
                readOnly = readOnly,
                textStyle = AppTheme.typography.body1,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = textColor,
                    backgroundColor = Color.Transparent,
                    unfocusedLabelColor = placeholderColor,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    focusedLabelColor = focusedColor,
                    cursorColor = focusedColor,
                    errorCursorColor = focusedColor,
                    placeholderColor = placeholderColor,
                    disabledTextColor = textColor,
                    disabledLabelColor = placeholderColor,
                    disabledPlaceholderColor = placeholderColor
                ),
                interactionSource = interactionSource,
                shape = shape
            )
        }

        if (state.message != null) {
            Text(
                text = state.message,
                color = assistiveTextColor,
                style = AppTheme.typography.caption1,
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(
                        start = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing),
                        end = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing),
                        top = 8.dp
                    )
            )
        }
    }
}

@Preview
@Composable
fun OutlinedTextInput_Preview() {
    AppTheme {
        AppSurface {
            OutlinedTextInput(
                value = "",
                label = "Home Address",
                onValueChange = {},
                placeholder = "Please enter your address",
                unfocusedTrailingIcon = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_search)
            )
        }
    }
}

@Preview
@Composable
fun OutlinedTextInput_Error_Preview() {
    AppTheme {
        AppSurface {
            OutlinedTextInput(
                value = "",
                label = "Home Address",
                onValueChange = {},
                placeholder = "Please enter your address",
                unfocusedTrailingIcon = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_search),
                state = TextInputState.Error("Test Error Message")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClearFocusOnKeyboardDismiss() {
    val imeIsVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    LaunchedEffect(imeIsVisible) {
        if (!imeIsVisible) {
            focusManager.clearFocus()
        }
    }
}
