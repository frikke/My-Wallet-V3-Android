package com.blockchain.componentlib.system

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Dialogue(
    body: String,
    firstButton: DialogueButton,
    secondButton: DialogueButton? = null
) {
    DialogueCard(
        body = body,
        firstButton = firstButton,
        secondButton = secondButton
    )
}

@Preview
@Composable
fun DialoguePreview() {
    AppTheme {
        Dialogue(
            body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ",
            firstButton = DialogueButton("Button 1", false, {})
        )
    }
}
