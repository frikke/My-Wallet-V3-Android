package com.blockchain.componentlib.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.image.Image
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark200
import com.blockchain.componentlib.theme.Dark400
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Dark700
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey300
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey600

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: String? = null,
    placeholder: String? = null,
    assistiveText: String? = null,
    leadingIcon: ImageResource = ImageResource.None,
    trailingIcon: ImageResource = ImageResource.None,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {

    val unfocusedColor = if (isError) {
        AppTheme.colors.error
    } else {
        if (!isSystemInDarkTheme()) {
            Grey300
        } else {
            Dark600
        }
    }

    val focusedColor = if (isError) {
        AppTheme.colors.error
    } else {
        AppTheme.colors.primary
    }

    val textColor = if (enabled) {
        AppTheme.colors.title
    } else {
        AppTheme.colors.muted
    }

    val backgroundColor = if (enabled) {
        AppTheme.colors.light
    } else {
        if (!isSystemInDarkTheme()) {
            Grey100
        } else {
            Dark700
        }
    }

    val placeholderColor = if (!isSystemInDarkTheme()) {
        Grey400
    } else {
        Dark200
    }

    val disabledPlaceholderColor = if (!isSystemInDarkTheme()) {
        Grey400
    } else {
        Dark400
    }

    val assistiveTextColor = if (!isSystemInDarkTheme()) {
        Grey600
    } else {
        Color.White
    }

    Column {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(1f),
            label = if (label != null) {
                { Text(label) }
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
                    Image(imageResource = leadingIcon)
                }
            } else null,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
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
                disabledPlaceholderColor = disabledPlaceholderColor
            )
        )

        if (isError) {
            Text(
                text = errorMessage ?: "",
                color = AppTheme.colors.error,
                style = AppTheme.typography.caption1,
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
        } else if (assistiveText != null) {
            Text(
                text = assistiveText,
                color = assistiveTextColor,
                style = AppTheme.typography.caption1,
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
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
                isError = true,
                errorMessage = "Test Error Message"
            )
        }
    }
}
