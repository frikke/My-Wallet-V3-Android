package com.blockchain.presentation.backup.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.presentation.backup.BackUpStatus
import com.blockchain.presentation.backup.BackupOption
import com.blockchain.presentation.backup.BackupPhraseArgs
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.BackupPhraseModelState
import com.blockchain.presentation.backup.BackupPhraseViewState
import com.blockchain.presentation.backup.CopyState
import com.blockchain.presentation.backup.FlowState
import com.blockchain.presentation.backup.UserMnemonicVerificationStatus
import com.blockchain.presentation.backup.navigation.BackupPhraseNavigationEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import piuk.blockchain.androidcore.utils.EncryptedPrefs
import java.util.concurrent.TimeUnit

class BackupPhraseViewModel(
    private val backupPhraseService: BackupPhraseService,
    private val backupPrefs: EncryptedPrefs
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
                showLoading = isLoading,
                showError = isError,
                mnemonic = mnemonic,
                backUpStatus = if (hasBackup) BackUpStatus.BACKED_UP else BackUpStatus.NO_BACKUP,
                copyState = copyState,
                mnemonicVerificationStatus = mnemonicVerificationStatus,
                flowState = flowState
            )
        }
    }

    override suspend fun handleIntent(modelState: BackupPhraseModelState, intent: BackupPhraseIntent) {
        when (intent) {
            BackupPhraseIntent.StartBackup -> {
                navigate(BackupPhraseNavigationEvent.BackupPhraseIntro)
            }

            BackupPhraseIntent.LoadData -> {
                loadData()
            }

            BackupPhraseIntent.StartBackupProcess -> {
                navigate(BackupPhraseNavigationEvent.RecoveryPhrase)
            }

            BackupPhraseIntent.EnableCloudBackup -> {
                updateState { it.copy(backupOption = BackupOption.CLOUD) }
                confirmRecoveryPhraseBackup()
            }

            BackupPhraseIntent.StartManualBackup -> {
                navigate(BackupPhraseNavigationEvent.ManualBackup)
            }

            BackupPhraseIntent.MnemonicCopied -> {
                resetCopyState()
                updateState { it.copy(copyState = CopyState.COPIED) }
            }

            BackupPhraseIntent.ResetCopy -> {
                updateState { it.copy(copyState = CopyState.IDLE) }
            }

            BackupPhraseIntent.StartUserPhraseVerification -> {
                navigate(BackupPhraseNavigationEvent.VerifyPhrase)
            }

            is BackupPhraseIntent.VerifyPhrase -> {
                updateState { it.copy(backupOption = BackupOption.MANUAL) }
                verifyPhraseAndConfirmBackup(intent.userMnemonic)
            }

            BackupPhraseIntent.PhraseVerified -> {
                when (modelState.backupOption) {
                    BackupOption.CLOUD -> {
                        backupPrefs.backupEnabled = true
                        navigate(BackupPhraseNavigationEvent.CloudBackupConfirmation)
                    }

                    BackupOption.MANUAL -> {
                        updateState { it.copy(isLoading = false) }
                        navigate(BackupPhraseNavigationEvent.BackupConfirmation)
                    }

                    BackupOption.NONE -> {
                    }
                }.exhaustive
            }

            BackupPhraseIntent.ResetVerificationStatus -> {
                updateState { it.copy(mnemonicVerificationStatus = UserMnemonicVerificationStatus.IDLE) }
            }

            BackupPhraseIntent.GoToPreviousScreen -> {
                navigate(BackupPhraseNavigationEvent.GoToPreviousScreen)
            }

            is BackupPhraseIntent.EndFlow -> {
                updateState { it.copy(flowState = FlowState.Ended(intent.isSuccessful)) }
            }
        }.exhaustive
    }

    fun isBackedUp() = backupPhraseService.isBackedUp()

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
                updateState { modelState.copy(isError = false) }
            }
            .doOnFailure {
                updateState { modelState.copy(isError = true) }
            }
    }

    private fun resetCopyState() {
        viewModelScope.launch {
            delay(TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES))
            onIntent(BackupPhraseIntent.ResetCopy)
            // todo reset clipboard
        }
    }

    private fun verifyPhraseAndConfirmBackup(userMnemonic: List<String>) {
        if (userMnemonic != modelState.mnemonic) {
            updateState { it.copy(mnemonicVerificationStatus = UserMnemonicVerificationStatus.INCORRECT) }
        } else {
            confirmRecoveryPhraseBackup()
        }
    }

    private fun confirmRecoveryPhraseBackup() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            backupPhraseService.confirmRecoveryPhraseBackedUp()
                .doOnSuccess {
                    onIntent(BackupPhraseIntent.PhraseVerified)
                }
                .doOnFailure {
                    updateState { it.copy(isLoading = false) }
                }
        }
    }
}
