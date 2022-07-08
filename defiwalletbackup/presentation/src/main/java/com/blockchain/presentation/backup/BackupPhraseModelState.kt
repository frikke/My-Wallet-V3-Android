package com.blockchain.presentation.backup

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class BackupPhraseModelState(
    val secondPassword: String? = null,
    val hasBackup: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val mnemonic: List<String> = emptyList(),
    val copyState: CopyState = CopyState.Idle(resetClipboard = false),
    val mnemonicVerificationStatus: UserMnemonicVerificationStatus = UserMnemonicVerificationStatus.IDLE,
    val flowState: FlowState = FlowState.InProgress
) : ModelState

enum class BackupOption {
    CLOUD, MANUAL
}
