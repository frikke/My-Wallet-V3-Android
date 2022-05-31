package com.blockchain.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.theme.Green000
import com.blockchain.componentlib.theme.Green600
import com.blockchain.componentlib.theme.Orange000
import com.blockchain.componentlib.theme.Orange600

data class DefaultPhraseViewState(
    val showProgress: Boolean,
    val mnemonic: List<String>,
    val mnemonicString: String,
    val backUpStatus: BackUpStatus,
    val copyState: CopyState
) : ViewState

enum class BackUpStatus(
    @DrawableRes val icon: Int,
    val bgColor: Color,
    val textColor: Color,
    @StringRes val text: Int
) {
    NO_BACKUP(
        icon = R.drawable.ic_alert,
        bgColor = Orange000,
        textColor = Orange600,
        text = R.string.back_up_splash_status_negative
    ),

    BACKED_UP(
        icon = R.drawable.ic_check,
        bgColor = Green000,
        textColor = Green600,
        text = R.string.back_up_splash_status_positive
    )
}

sealed interface CopyState {
    object Idle : CopyState
    object Copied : CopyState
}
