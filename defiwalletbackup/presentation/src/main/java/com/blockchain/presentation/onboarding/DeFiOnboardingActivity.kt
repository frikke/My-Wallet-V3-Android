package com.blockchain.presentation.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.BackupPhraseActivity
import com.blockchain.presentation.onboarding.navigation.DeFiOnboardingNavHost
import com.blockchain.presentation.onboarding.viewmodel.DeFiOnboardingViewModel
import com.blockchain.ui.password.SecondPasswordHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope

class DeFiOnboardingActivity : BlockchainActivity(), KoinScopeComponent {

    override val alwaysDisableScreenshots: Boolean = true

    override val scope: Scope = payloadScope
    val viewModel: DeFiOnboardingViewModel by viewModel()

    // pin verification
    private val secondPasswordHandler: SecondPasswordHandler by inject()
    private val onVerifyPinResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            pinCodeVerified()
        }
    }

    // backup phrase
    private val onBackupPhraseResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            /**
             * IMPORTANT
             *
             * mandatory Dispatchers.IO otherwise the nav event is not caught
             * has to do with running things serially in main thread where [Lifecycle.repeatOnLifecycle]
             * is supposed to start collecting
             *
             * [Lifecycle.repeatOnLifecycle] with [Lifecycle.State.RESUMED] should've been enough
             * so not sure if it's a lifecycle edge case
             */
            var job: Job? = null
            job = lifecycleScope.launch(Dispatchers.IO) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    backupPhraseComplete()
                    job?.cancel()
                }
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
                            shouldVerifyPin -> {
                                launchPinVerification()
                                viewModel.onIntent(DeFiOnboardingIntent.PinVerificationRequested)
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

    // pin verification
    private fun launchPinVerification() {
        launchPhraseBackup()

        // todo(othman)
        //        onVerifyPinResult.launch(
        //            PinActivity.newIntent(
        //                context = requireContext(),
        //                startForResult = true,
        //                originScreen = PinActivity.Companion.OriginScreenToPin.BACKUP_PHRASE,
        //                addFlagsToClear = false
        //            )
        //        )
    }

    private fun pinCodeVerified() {
        secondPasswordHandler.validate(
            this,
            object : SecondPasswordHandler.ResultListener {
                override fun onNoSecondPassword() {
                    launchPhraseBackup()
                }

                override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                    launchPhraseBackup(secondPassword = validatedSecondPassword)
                }
            }
        )
    }

    // phrase backup
    private fun launchPhraseBackup(secondPassword: String? = null) {
        onBackupPhraseResult.launch(
            BackupPhraseActivity.newIntent(context = this, secondPassword = secondPassword)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        )
    }

    private fun backupPhraseComplete() {
        viewModel.onIntent(DeFiOnboardingIntent.BackupPhraseComplete)
    }

    private fun finish(isSuccessful: Boolean) {
        setResult(if (isSuccessful) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, DeFiOnboardingActivity::class.java)
    }
}
