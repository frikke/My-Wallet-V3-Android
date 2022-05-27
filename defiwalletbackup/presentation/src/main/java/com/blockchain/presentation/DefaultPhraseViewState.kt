package com.blockchain.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class DefaultPhraseViewState(
    val showProgress: Boolean,
    val mnemonic: List<String>,
    val mnemonicString: String,
    val warning: BackUpPhraseWarning = BackUpPhraseWarning.NONE,
    val copyState: CopyState
) : ViewState

enum class BackUpPhraseWarning {
    NONE, NO_BACKUP
}

sealed interface CopyState {
    object Idle : CopyState
    object Copied : CopyState
}