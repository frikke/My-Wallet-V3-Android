package com.blockchain.presentation.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.presentation.CopyState

data class DefaultPhraseModelState(
    val isLoading: Boolean = false,
    val mnemonic: List<String> = emptyList(),
    val hasBackup: Boolean = false,
    val copyState: CopyState = CopyState.Idle
) : ModelState
