package com.blockchain.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class BackupPhraseModelState(
    val secondPassword: String? = null,
    val hasBackup: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val mnemonic: List<String> = emptyList(),
    val copyState: CopyState = CopyState.IDLE,
    val mnemonicVerificationStatus: UserMnemonicVerificationStatus = UserMnemonicVerificationStatus.NO_STATUS,
    val flowStatus: FlowStatus = FlowStatus.InProgress
) : ModelState
