package com.blockchain.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import kotlinx.parcelize.Parcelize

@Parcelize
data class BackupPhraseArgs(
    val isBackedUp: Boolean,
    val secondPassword: String?
) : ModelConfigArgs.ParcelableArgs {
    companion object {
        const val ARGS_KEY: String = "BackupPhraseArgs"
    }
}
