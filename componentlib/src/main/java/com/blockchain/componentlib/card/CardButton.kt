package com.blockchain.componentlib.card

import androidx.compose.ui.graphics.Color

data class CardButton(
    val text: String,
    val backgroundColor: Color? = null,
    val type: ButtonType = ButtonType.Primary,
    val onClick: () -> Unit = {}
)

enum class ButtonType {
    Primary,
    Secondary,
    Minimal
}
