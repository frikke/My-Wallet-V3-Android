package com.blockchain.home.presentation.activity.detail.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.home.presentation.activity.detail.ActivityDetailItemState
import com.blockchain.home.presentation.activity.detail.ButtonStyle

@Composable
fun ActivityDetailButton(
    data: ActivityDetailItemState.Button
) {
    val text = data.value
    val action: (() -> Unit) = {

    }
    val modifier = Modifier.fillMaxWidth()

    when (data.style) {
        ButtonStyle.Primary -> {
            PrimaryButton(
                modifier = modifier,
                text = text,
                onClick = action
            )
        }
        ButtonStyle.Secondary -> {
            TertiaryButton(
                modifier = modifier,
                text = text,
                onClick = action
            )
        }
    }
}

@Preview
@Composable
fun PreviewActivityDetailButton_Primary() {
    ActivityDetailButton(
        data = ActivityDetailItemState.Button(
            value = "primary",
            style = ButtonStyle.Primary
        )
    )
}

@Preview
@Composable
fun PreviewActivityDetailButton_Secondary() {
    ActivityDetailButton(
        data = ActivityDetailItemState.Button(
            value = "primary",
            style = ButtonStyle.Secondary
        )
    )
}