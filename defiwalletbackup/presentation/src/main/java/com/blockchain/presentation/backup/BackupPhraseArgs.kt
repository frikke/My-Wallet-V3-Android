package com.blockchain.presentation.backup

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import kotlinx.parcelize.Parcelize

@Parcelize
data class BackupPhraseArgs(
    val secondPassword: String?
) : ModelConfigArgs.ParcelableArgs {
    companion object {
        const val ARGS_KEY: String = "BackupPhraseArgs"
    }
}
