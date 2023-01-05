package com.blockchain.presentation.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.setContent
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.backup.BackupPhraseActivity
import com.blockchain.presentation.onboarding.navigation.DeFiOnboardingNavHost
import com.blockchain.presentation.onboarding.viewmodel.DeFiOnboardingViewModel
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class DeFiOnboardingActivity : BlockchainActivity(), KoinScopeComponent {

    override val alwaysDisableScreenshots: Boolean = true

    override val statusbarColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL_ONLY)

    override val scope: Scope = payloadScope
    val viewModel: DeFiOnboardingViewModel by viewModel()

    // backup phrase
    private val onBackupPhraseResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            /**
             * IMPORTANT
             *
             * mandatory Dispatchers.IO otherwise the nav event is not caught as we're coming back from another activity
             * has to do with running things serially in main thread where [Lifecycle.repeatOnLifecycle]
             * is supposed to start collecting
             */
            lifecycleScope.launch(Dispatchers.IO) {
                lifecycleScope.launchWhenStarted { backupPhraseComplete() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collectViewState()

        setContent {
            DeFiOnboardingNavHost(viewModel)
        }
    }

    private fun collectViewState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect { viewState ->
                    with(viewState) {
                        when {
                            shouldLaunchPhraseBackup -> {
                                launchPhraseBackup()
                                viewModel.onIntent(DeFiOnboardingIntent.PhraseBackupRequested)
                            }

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

    private fun launchPhraseBackup() {
        onBackupPhraseResult.launch(
            BackupPhraseActivity
                .newIntent(
                    context = this,
                    allowSkipBackup = true
                )
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        )
    }

    private fun backupPhraseComplete() {
        finish(isSuccessful = true)
    }

    private fun finish(isSuccessful: Boolean) {
        setResult(if (isSuccessful) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, DeFiOnboardingActivity::class.java)
    }
}
