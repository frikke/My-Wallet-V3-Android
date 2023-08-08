package com.blockchain.presentation.backup.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.core.utils.EncryptedPrefs
import com.blockchain.defiwalletbackup.domain.analytics.BackupPhraseAnalytics
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatusPrefs
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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackupPhraseViewModel(
    private val backupPhraseService: BackupPhraseService,
    private val settingsDataManager: SettingsDataManager,
    private val backupPrefs: EncryptedPrefs,
    private val analytics: Analytics,
    private val walletStatusPrefs: WalletStatusPrefs,
    private val authPrefs: AuthPrefs
) : MviViewModel<
    BackupPhraseIntent,
    BackupPhraseViewState,
    BackupPhraseModelState,
    BackupPhraseNavigationEvent,
    BackupPhraseArgs
    >(
    initialState = BackupPhraseModelState()
) {
    override fun viewCreated(args: BackupPhraseArgs) {
        updateState {
            copy(
                secondPassword = args.secondPassword,
                allowSkipBackup = args.allowSkipBackup
            )
        }

        onIntent(BackupPhraseIntent.LoadData)
    }

    override fun BackupPhraseModelState.reduce() = BackupPhraseViewState(
        showSkipBackup = allowSkipBackup,
        showLoading = isLoading,
        showError = isError,
        mnemonic = mnemonic,
        backUpStatus = if (hasBackup) BackUpStatus.BACKED_UP else BackUpStatus.NO_BACKUP,
        copyState = copyState,
        mnemonicVerificationStatus = mnemonicVerificationStatus,
        flowState = flowState
    )

    override suspend fun handleIntent(modelState: BackupPhraseModelState, intent: BackupPhraseIntent) {
        when (intent) {
            BackupPhraseIntent.TriggerEmailAlert -> {
                triggerEmailAlert()
            }

            BackupPhraseIntent.StartBackup -> {
                navigate(BackupPhraseNavigationEvent.BackupPhraseIntro)
            }

            BackupPhraseIntent.LoadData -> {
                loadData()
            }

            BackupPhraseIntent.GoToSkipBackup -> {
                navigate(BackupPhraseNavigationEvent.SkipBackup)
            }

            BackupPhraseIntent.SkipBackup -> {
                markBackupAsSkipped()
                onIntent(BackupPhraseIntent.EndFlow(isSuccessful = true))
            }

            BackupPhraseIntent.StartBackupProcess -> {
                analytics.logEvent(BackupPhraseAnalytics.enableDefiClicked)
                navigate(BackupPhraseNavigationEvent.RecoveryPhrase)
            }

            BackupPhraseIntent.EnableCloudBackup -> {
                confirmRecoveryPhraseBackup(BackupOption.CLOUD)
            }

            BackupPhraseIntent.StartManualBackup -> {
                navigate(BackupPhraseNavigationEvent.ManualBackup)
            }

            BackupPhraseIntent.MnemonicCopied -> {
                resetCopyState()
                updateState { copy(copyState = CopyState.Copied) }
            }

            BackupPhraseIntent.ResetCopy -> {
                updateState { copy(copyState = CopyState.Idle(resetClipboard = true)) }
            }

            BackupPhraseIntent.ClipboardReset -> {
                updateState { copy(copyState = CopyState.Idle(resetClipboard = false)) }
            }

            BackupPhraseIntent.StartUserPhraseVerification -> {
                navigate(BackupPhraseNavigationEvent.VerifyPhrase)
            }

            is BackupPhraseIntent.VerifyPhrase -> {
                verifyPhraseAndConfirmBackup(intent.userMnemonic)
            }

            BackupPhraseIntent.ResetVerificationStatus -> {
                updateState { copy(mnemonicVerificationStatus = UserMnemonicVerificationStatus.IDLE) }
            }

            BackupPhraseIntent.GoToPreviousScreen -> {
                navigate(BackupPhraseNavigationEvent.GoToPreviousScreen)
            }

            is BackupPhraseIntent.EndFlow -> {
                updateState { copy(flowState = FlowState.Ended(intent.isSuccessful)) }
            }
        }.exhaustive
    }

    private fun triggerEmailAlert() {
        viewModelScope.launch {
            settingsDataManager.triggerEmailAlert(
                guid = authPrefs.walletGuid,
                sharedKey = authPrefs.sharedKey
            )
        }
    }

    fun isBackedUp() = backupPhraseService.isBackedUp()

    private fun loadData() {
        loadBackupStatus()
        loadRecoveryPhrase()
    }

    private fun loadBackupStatus() {
        updateState {
            copy(
                hasBackup = backupPhraseService.isBackedUp(),
                allowSkipBackup = allowSkipBackup && backupPhraseService.isBackedUp().not()
            )
        }
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

    private fun markBackupAsSkipped() {
        walletStatusPrefs.isWalletBackUpSkipped = true
    }

    private fun resetCopyState() {
        viewModelScope.launch {
            delay(TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES))
            onIntent(BackupPhraseIntent.ResetCopy)
        }
    }

    private fun verifyPhraseAndConfirmBackup(userMnemonic: List<String>) {
        if (userMnemonic != modelState.mnemonic) {
            updateState { copy(mnemonicVerificationStatus = UserMnemonicVerificationStatus.INCORRECT) }
        } else {
            confirmRecoveryPhraseBackup(BackupOption.MANUAL)
        }
    }

    private fun confirmRecoveryPhraseBackup(backupOption: BackupOption) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            backupPhraseService.confirmRecoveryPhraseBackedUp()
                .doOnSuccess {
                    updateState { copy(isLoading = false) }

                    when (backupOption) {
                        BackupOption.CLOUD -> {
                            backupPrefs.backupEnabled = true
                            navigate(BackupPhraseNavigationEvent.CloudBackupConfirmation)
                        }

                        BackupOption.MANUAL -> {
                            navigate(BackupPhraseNavigationEvent.BackupConfirmation)
                        }
                    }.exhaustive
                }
                .doOnFailure {
                    updateState { copy(isLoading = false) }
                }
        }
    }
}
