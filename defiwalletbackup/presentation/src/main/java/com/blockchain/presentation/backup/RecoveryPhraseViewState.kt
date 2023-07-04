package com.blockchain.presentation.backup

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

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

enum class BackUpStatus {
    NO_BACKUP, BACKED_UP
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
