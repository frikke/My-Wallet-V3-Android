package com.blockchain.componentlib.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
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

    Column {
        TextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            modifier = modifier
                .fillMaxWidth(1f)
                .onFocusChanged { focusState ->
                    onFocusChanged.invoke(focusState)
                },
            label = if (label != null) {
                { Text(label, style = AppTheme.typography.caption1) }
            } else null,
            placeholder = if (placeholder != null) {
                { Text(placeholder) }
            } else null,
            leadingIcon = if (leadingIcon != ImageResource.None) {
                {
                    Image(imageResource = leadingIcon)
                }
            } else null,
            trailingIcon = if (trailingIcon != ImageResource.None) {
                {
                    Image(
                        modifier = Modifier.clickable {
                            onTrailingIconClicked.invoke()
                        },
                        imageResource = trailingIcon
                    )
                }
            } else null,
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

        if (state.message != null) {
            Text(
                text = state.message,
                color = assistiveTextColor,
                style = AppTheme.typography.caption1,
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(
                        start = dimensionResource(R.dimen.medium_margin),
                        end = dimensionResource(R.dimen.medium_margin),
                        top = 8.dp
                    )
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
                state = TextInputState.Error("Test Error Message")
            )
        }
    }
}

@Composable
fun OutlinedTextInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    state: TextInputState = TextInputState.Default(""),
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
        AppTheme.colors.background
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
        Box(
            modifier = Modifier
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                .background(color = backgroundColor)
        ) {
            TextField(
                value = value,
                onValueChange = { onValueChange(it.take(maxLength)) },
                modifier = modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        onFocusChanged.invoke(focusState)
                        hasFocus.value = focusState.isFocused
                    },
                label = if (label != null) {
                    { Text(label, style = AppTheme.typography.caption1) }
                } else null,
                placeholder = if (placeholder != null) {
                    {
                        SimpleText(
                            text = placeholder,
                            style = ComposeTypographies.Body1,
                            gravity = ComposeGravities.Start,
                            color = ComposeColors.Muted
                        )
                    }
                } else null,
                leadingIcon = if (leadingIcon != ImageResource.None) {
                    {
                        Image(imageResource = leadingIcon)
                    }
                } else null,
                trailingIcon = if (trailingIcon != ImageResource.None) {
                    {
                        Image(
                            modifier = Modifier.clickable {
                                onTrailingIconClicked.invoke()
                            },
                            imageResource = trailingIcon
                        )
                    }
                } else null,
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
                shape = RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp)
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
                        start = dimensionResource(R.dimen.medium_margin),
                        end = dimensionResource(R.dimen.medium_margin),
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
                unfocusedTrailingIcon = ImageResource.Local(R.drawable.ic_search),
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
                unfocusedTrailingIcon = ImageResource.Local(R.drawable.ic_search),
                state = TextInputState.Error("Test Error Message")
            )
        }
    }
}
