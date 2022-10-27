package com.blockchain.home.presentation.activity.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.button.TertiaryButton

// button
enum class ActivityButtonStyle {
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
        ActivityButtonStyle.Primary -> {
            PrimaryButton(
                modifier = modifier,
                text = text,
                onClick = onClick
            )
        }
        ActivityButtonStyle.Secondary -> {
            SecondaryButton(
                modifier = modifier,
                text = text,
                onClick = onClick
            )
        }
        ActivityButtonStyle.Tertiary -> {
            TertiaryButton(
                modifier = modifier,
                text = text,
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
            value = "Primary",
            style = ActivityButtonStyle.Primary
        ),
        onClick = {}
    )
}

@Preview
@Composable
fun PreviewActivityDetailButton_Secondary() {
    ActivityDetailButton(
        data = ActivityComponent.Button(
            value = "Secondary",
            style = ActivityButtonStyle.Secondary
        ),
        onClick = {}
    )
}

@Preview
@Composable
fun PreviewActivityDetailButton_Tertiary() {
    ActivityDetailButton(
        data = ActivityComponent.Button(
            value = "Tertiary",
            style = ActivityButtonStyle.Tertiary
        ),
        onClick = {}
    )
}
