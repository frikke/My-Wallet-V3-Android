package com.blockchain.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.blockchain.commonarch.presentation.base.BlockchainActivity

class BackupPhraseActivity : BlockchainActivity() {

    override val alwaysDisableScreenshots: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BackupPhraseNavHost(
                backupPhraseArgs = intent.getParcelableExtra(BackupPhraseArgs.ARGS_KEY)
                    ?: error("missing DefaultPhraseArgs")
            )
        }
    }

    companion object {
        fun newIntent(context: Context, secondPassword: String?): Intent =
            Intent(context, BackupPhraseActivity::class.java).apply {
                putExtra(BackupPhraseArgs.ARGS_KEY, BackupPhraseArgs(secondPassword))
            }
    }
}
