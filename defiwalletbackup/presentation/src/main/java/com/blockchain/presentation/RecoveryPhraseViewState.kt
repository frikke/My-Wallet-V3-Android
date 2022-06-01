package com.blockchain.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.theme.Green000
import com.blockchain.componentlib.theme.Green600
import com.blockchain.componentlib.theme.Orange000
import com.blockchain.componentlib.theme.Orange600

const val TOTAL_STEP_COUNT = 2

data class BackupPhraseViewState(
    val showLoading: Boolean,
    val isError: Boolean,
    val mnemonic: List<String>,
    val backUpStatus: BackUpStatus,
    val copyState: CopyState,
    val mnemonicVerificationStatus: UserMnemonicVerificationStatus,
    val flowStatus: FlowStatus
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
        text = R.string.back_up_status_negative
    ),

    BACKED_UP(
        icon = R.drawable.ic_check,
        bgColor = Green000,
        textColor = Green600,
        text = R.string.back_up_status_positive
    )
}

enum class CopyState {
    IDLE, COPIED
}

enum class UserMnemonicVerificationStatus {
    NO_STATUS, VERIFIED, INCORRECT
}

sealed interface FlowStatus {
    object InProgress : FlowStatus
    data class Ended(val isSuccessful: Boolean) : FlowStatus
}