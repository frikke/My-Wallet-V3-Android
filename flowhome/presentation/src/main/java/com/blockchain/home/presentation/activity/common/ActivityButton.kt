package com.blockchain.home.presentation.activity.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.value

// button
enum class ActivityButtonStyleState {
    Primary,
    Secondary,
    Tertiary
}

@Composable
fun ActivityDetailButton(
    data: ActivityComponent.Button,
    onClick: (() -> Unit)? = null
) {
    val text = data.value
    val modifier = Modifier.fillMaxWidth()
    val onClick: () -> Unit = { onClick?.invoke() }

    when (data.style) {
        ActivityButtonStyleState.Primary -> {
            PrimaryButton(
                modifier = modifier,
                text = text.value(),
                onClick = onClick
            )
        }
        ActivityButtonStyleState.Secondary -> {
            SecondaryButton(
                modifier = modifier,
                text = text.value(),
                onClick = onClick
            )
        }
        ActivityButtonStyleState.Tertiary -> {
            TertiaryButton(
                modifier = modifier,
                text = text.value(),
                onClick = onClick
            )
        }
    }
}

@Preview
@Composable
fun PreviewActivityDetailButton_Primary() {
    ActivityDetailButton(
        data = ActivityComponent.Button(
            value = TextValue.StringValue("Primary"),
            style = ActivityButtonStyleState.Primary
        ),
        onClick = {}
    )
}

@Preview
@Composable
fun PreviewActivityDetailButton_Secondary() {
    ActivityDetailButton(
        data = ActivityComponent.Button(
            value = TextValue.StringValue("Secondary"),
            style = ActivityButtonStyleState.Secondary
        ),
        onClick = {}
    )
}

@Preview
@Composable
fun PreviewActivityDetailButton_Tertiary() {
    ActivityDetailButton(
        data = ActivityComponent.Button(
            value = TextValue.StringValue("Tertiary"),
            style = ActivityButtonStyleState.Tertiary
        ),
        onClick = {}
    )
}
