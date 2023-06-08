package com.blockchain.home.presentation.activity.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.value
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle

// button
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
                text = text.value(),
                onClick = onClick
            )
        }
        ActivityButtonStyle.Secondary -> {
            SecondaryButton(
                modifier = modifier,
                text = text.value(),
                onClick = onClick
            )
        }
        ActivityButtonStyle.Tertiary -> {
            MinimalPrimaryButton(
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
            id = "",
            value = TextValue.StringValue("Primary"),
            style = ActivityButtonStyle.Primary,
            action = ActivityButtonAction(
                type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                data = ""
            )
        ),
        onClick = {}
    )
}

@Preview
@Composable
fun PreviewActivityDetailButton_Secondary() {
    ActivityDetailButton(
        data = ActivityComponent.Button(
            id = "",
            value = TextValue.StringValue("Secondary"),
            style = ActivityButtonStyle.Secondary,
            action = ActivityButtonAction(
                type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                data = ""
            )
        ),
        onClick = {}
    )
}

@Preview
@Composable
fun PreviewActivityDetailButton_Tertiary() {
    ActivityDetailButton(
        data = ActivityComponent.Button(
            id = "",
            value = TextValue.StringValue("Tertiary"),
            style = ActivityButtonStyle.Tertiary,
            action = ActivityButtonAction(
                type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                data = ""
            )
        ),
        onClick = {}
    )
}
