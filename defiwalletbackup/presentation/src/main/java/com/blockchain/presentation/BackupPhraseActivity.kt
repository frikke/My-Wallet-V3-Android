package com.blockchain.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.navigation.BackupPhraseNavHost
import com.blockchain.presentation.viewmodel.BackupPhraseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class BackupPhraseActivity : BlockchainActivity(), KoinScopeComponent {

    override val alwaysDisableScreenshots: Boolean = true

    override val scope: Scope = payloadScope
    val viewModel: BackupPhraseViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collectViewState()

        setContent {
            BackupPhraseNavHost(
                viewModel = viewModel,
                backupPhraseArgs = intent.getParcelableExtra(BackupPhraseArgs.ARGS_KEY)
                    ?: error("missing DefaultPhraseArgs")
            )
        }
    }

    private fun collectViewState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect { viewState ->
                    with(viewState) {
                        when {
                            flowState is FlowState.Ended -> finish(flowState.isSuccessful)
                            else -> {
                                /* n/a */
                            }
                        }.exhaustive
                    }
                }
            }
        }
    }

    private fun finish(isSuccessful: Boolean) {
        setResult(if (isSuccessful) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        fun newIntent(context: Context, isBackedUp: Boolean, secondPassword: String?): Intent =
            Intent(context, BackupPhraseActivity::class.java).apply {
                putExtra(
                    BackupPhraseArgs.ARGS_KEY,
                    BackupPhraseArgs(isBackedUp = isBackedUp, secondPassword = secondPassword)
                )
            }
    }
}
