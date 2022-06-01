package com.blockchain.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import com.blockchain.presentation.BackUpStatus
import com.blockchain.presentation.BackupPhraseArgs
import com.blockchain.presentation.BackupPhraseIntent
import com.blockchain.presentation.BackupPhraseModelState
import com.blockchain.presentation.BackupPhraseViewState
import com.blockchain.presentation.CopyState
import com.blockchain.presentation.navigation.BackupPhraseNavigationEvent
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackupPhraseViewModel : MviViewModel<BackupPhraseIntent,
    BackupPhraseViewState,
    BackupPhraseModelState,
    BackupPhraseNavigationEvent,
    BackupPhraseArgs>(
    initialState = BackupPhraseModelState()
) {
    override fun viewCreated(args: BackupPhraseArgs) {
        updateState { it.copy(secondPassword = args.secondPassword) }

        onIntent(BackupPhraseIntent.GetBackupStatus)
    }

    override fun reduce(state: BackupPhraseModelState): BackupPhraseViewState {
        return with(state) {
            BackupPhraseViewState(
                isLoading = isLoading,
                mnemonic = mnemonic,
                mnemonicString = mnemonic.joinToString(separator = " "),
                backUpStatus = if (hasBackup) BackUpStatus.BACKED_UP else BackUpStatus.NO_BACKUP,
                copyState = copyState,
                mnemonicVerificationStatus = mnemonicVerificationStatus
            )
        }
    }

    override suspend fun handleIntent(modelState: BackupPhraseModelState, intent: BackupPhraseIntent) {
        when (intent) {
            BackupPhraseIntent.GetBackupStatus -> {
                getBackupStatus()
            }

            BackupPhraseIntent.LoadRecoveryPhrase -> {
                updateState { modelState.copy(mnemonic = mnemonic()) }
            }

            BackupPhraseIntent.StartBackupProcess -> {
                navigate(BackupPhraseNavigationEvent.RecoveryPhrase)
            }

            BackupPhraseIntent.StartManualBackup -> {
                navigate(BackupPhraseNavigationEvent.ManualBackup)
            }

            BackupPhraseIntent.StartUserPhraseVerification -> {
                navigate(BackupPhraseNavigationEvent.VerifyPhrase)
            }

            BackupPhraseIntent.MnemonicCopied -> {
                resetCopyState()
                updateState { it.copy(copyState = CopyState.Copied) }
            }

            BackupPhraseIntent.ResetCopy -> {
                updateState { it.copy(copyState = CopyState.Idle) }
            }

            is BackupPhraseIntent.VerifyPhrase -> {
                verifyPhrase(intent.userMnemonic)
            }
        }.exhaustive
    }

    private fun getBackupStatus() {
        viewModelScope.launch {
            updateState { modelState.copy(hasBackup = false) }

            onIntent(BackupPhraseIntent.LoadRecoveryPhrase)
        }
    }

    private fun mnemonic(): List<String> {
        val locales = Locale.getISOCountries().toList()
        return locales.map {
            Locale("", it).isO3Country
        }.shuffled().subList(0, 12)
    }

    private fun resetCopyState() {
        viewModelScope.launch {
            delay(TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES))
            onIntent(BackupPhraseIntent.ResetCopy)
            // todo(othman) reset clipboard
        }
    }

    private fun verifyPhrase(userMnemonic: List<String>) {
        // todo(othman) verify phrase
        navigate(BackupPhraseNavigationEvent.BackupConfirmation)
    }
}
