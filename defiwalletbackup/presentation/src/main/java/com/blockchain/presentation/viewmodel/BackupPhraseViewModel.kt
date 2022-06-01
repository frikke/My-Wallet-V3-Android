package com.blockchain.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.presentation.BackUpStatus
import com.blockchain.presentation.BackupPhraseArgs
import com.blockchain.presentation.BackupPhraseIntent
import com.blockchain.presentation.BackupPhraseModelState
import com.blockchain.presentation.BackupPhraseViewState
import com.blockchain.presentation.CopyState
import com.blockchain.presentation.FlowStatus
import com.blockchain.presentation.UserMnemonicVerificationStatus
import com.blockchain.presentation.navigation.BackupPhraseNavigationEvent
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackupPhraseViewModel(
    private val backupPhraseService: BackupPhraseService
) : MviViewModel<BackupPhraseIntent,
    BackupPhraseViewState,
    BackupPhraseModelState,
    BackupPhraseNavigationEvent,
    BackupPhraseArgs>(
    initialState = BackupPhraseModelState()
) {
    override fun viewCreated(args: BackupPhraseArgs) {
        updateState { it.copy(secondPassword = args.secondPassword) }

        onIntent(BackupPhraseIntent.LoadData)
    }

    override fun reduce(state: BackupPhraseModelState): BackupPhraseViewState {
        return with(state) {
            BackupPhraseViewState(
                isLoading = isLoading,
                mnemonic = mnemonic,
                mnemonicString = mnemonic.joinToString(separator = " "),
                backUpStatus = if (hasBackup) BackUpStatus.BACKED_UP else BackUpStatus.NO_BACKUP,
                copyState = copyState,
                mnemonicVerificationStatus = mnemonicVerificationStatus,
                flowStatus = flowStatus
            )
        }
    }

    override suspend fun handleIntent(modelState: BackupPhraseModelState, intent: BackupPhraseIntent) {
        when (intent) {
            BackupPhraseIntent.LoadData -> {
                loadData()
            }

            BackupPhraseIntent.StartBackupProcess -> {
                navigate(BackupPhraseNavigationEvent.RecoveryPhrase)
            }

            BackupPhraseIntent.StartManualBackup -> {
                navigate(BackupPhraseNavigationEvent.ManualBackup)
            }

            BackupPhraseIntent.MnemonicCopied -> {
                resetCopyState()
                updateState { it.copy(copyState = CopyState.Copied) }
            }

            BackupPhraseIntent.ResetCopy -> {
                updateState { it.copy(copyState = CopyState.Idle) }
            }

            BackupPhraseIntent.StartUserPhraseVerification -> {
                navigate(BackupPhraseNavigationEvent.VerifyPhrase)
            }

            is BackupPhraseIntent.VerifyPhrase -> {
                verifyPhrase(intent.userMnemonic)
            }

            BackupPhraseIntent.GoToPreviousScreen -> {
                navigate(BackupPhraseNavigationEvent.GoToPreviousScreen)
            }

            is BackupPhraseIntent.EndFlow -> {
                updateState { it.copy(flowStatus = FlowStatus.Ended(intent.isSuccessful)) }
            }
        }.exhaustive
    }

    private fun loadData() {
        loadBackupStatus()
        loadRecoveryPhrase()
    }

    private fun loadBackupStatus() {
        updateState { modelState.copy(hasBackup = backupPhraseService.isBackedUp()) }
    }

    /**
     * Load mnemonic phrase - or show an error when not found to cancel
     */
    private fun loadRecoveryPhrase() {
        backupPhraseService.getMnemonic(modelState.secondPassword)
            .doOnSuccess { mnemonic ->
                updateState { modelState.copy(mnemonic = mnemonic) }
            }
            .doOnFailure {
                // todo show error
            }
    }

    private fun resetCopyState() {
        viewModelScope.launch {
            delay(TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES))
            onIntent(BackupPhraseIntent.ResetCopy)
            // todo reset clipboard
        }
    }

    private fun verifyPhrase(userMnemonic: List<String>) {
        if (userMnemonic != modelState.mnemonic) {
            // todo(othman): check with ethan how to show phrase is incorrect
            updateState { it.copy(mnemonicVerificationStatus = UserMnemonicVerificationStatus.INCORRECT) }
        } else {
            updateState { it.copy(mnemonicVerificationStatus = UserMnemonicVerificationStatus.VERIFIED) }

            // todo(othman): check with ethan how to move from "verify" to "next"
            viewModelScope.launch {
                updateState { it.copy(isLoading = true) }

                backupPhraseService.confirmRecoveryPhraseBackedUp()
                    .doOnSuccess {
                        updateState { it.copy(isLoading = false) }
                        navigate(BackupPhraseNavigationEvent.BackupConfirmation)
                    }
                    .doOnFailure {
                        updateState { it.copy(isLoading = false) }
                    }
            }
        }
    }
}
