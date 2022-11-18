package com.blockchain.presentation.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.utils.copyToClipboard
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.BackupPhrasePinService
import com.blockchain.presentation.backup.navigation.BackupPhraseNavHost
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope

class BackupPhraseActivity : BlockchainActivity(), KoinScopeComponent {

    override val alwaysDisableScreenshots: Boolean = true

    override val scope: Scope = payloadScope
    private val viewModel: BackupPhraseViewModel by viewModel()

    private val pinService: BackupPhrasePinService by inject()

    private val args: BackupPhraseArgs by lazy {
        intent.getParcelableExtra(BackupPhraseArgs.ARGS_KEY)
            ?: BackupPhraseArgs(secondPassword = null, allowSkipBackup = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pinService.init(this)

        lifecycleScope.launchWhenStarted { launchPinVerification() }
    }

    private fun initContent(secondPassword: String?) {
        collectViewState()

        setContent {
            BackupPhraseNavHost(
                viewModel = viewModel,
                backupPhraseArgs = args.copy(secondPassword = secondPassword)
            )
        }
    }

    private fun launchPinVerification() {
        pinService.verifyPin { successful, secondPassword ->
            if (successful) {
                viewModel.onIntent(BackupPhraseIntent.TriggerEmailAlert)
                initContent(secondPassword = secondPassword)
            } else {
                finish(isSuccessful = false)
            }
        }
    }

    private fun collectViewState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect { viewState ->
                    with(viewState) {
                        when {
                            copyState is CopyState.Idle && copyState.resetClipboard -> {
                                resetClipboard()
                                viewModel.onIntent(BackupPhraseIntent.ClipboardReset)
                            }

                            flowState is FlowState.Ended -> {
                                finish(flowState.isSuccessful)
                            }

                            else -> {
                                /* n/a */
                            }
                        }.exhaustive
                    }
                }
            }
        }
    }

    private fun resetClipboard() {
        copyToClipboard(
            label = "",
            text = ""
        )
    }

    private fun finish(isSuccessful: Boolean) {
        setResult(if (isSuccessful) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        resetClipboard()
    }

    companion object {
        fun newIntent(context: Context, allowSkipBackup: Boolean = false): Intent =
            Intent(context, BackupPhraseActivity::class.java).apply {
                putExtra(
                    BackupPhraseArgs.ARGS_KEY,
                    BackupPhraseArgs(
                        secondPassword = null,
                        allowSkipBackup = allowSkipBackup
                    )
                )
            }
    }
}
