package com.blockchain.presentation.backup

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.theme.Green000
import com.blockchain.componentlib.theme.Green700
import com.blockchain.componentlib.theme.Orange000
import com.blockchain.componentlib.theme.Orange600
import com.blockchain.presentation.R

const val TOTAL_STEP_COUNT = 2

data class BackupPhraseViewState(
    val showSkipBackup: Boolean,
    val showLoading: Boolean,
    val showError: Boolean,
    val mnemonic: List<String>,
    val backUpStatus: BackUpStatus,
    val copyState: CopyState,
    val mnemonicVerificationStatus: UserMnemonicVerificationStatus,
    val flowState: FlowState
) : ViewState

enum class BackUpStatus(
    @DrawableRes val icon: Int,
    val iconColor: Color,
    val bgColor: Color,
    val textColor: Color,
    @StringRes val text: Int
) {
    NO_BACKUP(
        icon = R.drawable.alert_on,
        iconColor = Orange600,
        bgColor = Orange000,
        textColor = Orange600,
        text = R.string.back_up_status_negative
    ),

    BACKED_UP(
        icon = R.drawable.check_on,
        iconColor = Green700,
        bgColor = Green000,
        textColor = Green700,
        text = R.string.back_up_status_positive
    )
}

sealed interface CopyState {
    data class Idle(val resetClipboard: Boolean) : CopyState
    object Copied : CopyState
}

enum class UserMnemonicVerificationStatus {
    IDLE, INCORRECT
}

sealed interface FlowState {
    object InProgress : FlowState
    data class Ended(val isSuccessful: Boolean) : FlowState
}
