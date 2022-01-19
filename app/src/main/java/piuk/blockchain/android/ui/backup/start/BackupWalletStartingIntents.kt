package piuk.blockchain.android.ui.backup.start

import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class BackupWalletStartingIntents : MviIntent<BackupWalletStartingState> {

    object TriggerEmailAlert : BackupWalletStartingIntents() {
        override fun reduce(oldState: BackupWalletStartingState): BackupWalletStartingState =
            oldState.copy(
                status = BackupWalletStartingStatus.SENDING_ALERT
            )
    }

    data class UpdateStatus(private val status: BackupWalletStartingStatus) : BackupWalletStartingIntents() {
        override fun reduce(oldState: BackupWalletStartingState): BackupWalletStartingState =
            oldState.copy(
                status = status
            )
    }
}
