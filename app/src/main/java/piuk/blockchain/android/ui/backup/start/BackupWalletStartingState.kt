package piuk.blockchain.android.ui.backup.start

import com.blockchain.commonarch.presentation.mvi.MviState

enum class BackupWalletStartingStatus {
    INIT,
    REQUEST_PIN,
    SENDING_ALERT,
    COMPLETE
}

data class BackupWalletStartingState(
    val status: BackupWalletStartingStatus = BackupWalletStartingStatus.INIT
) : MviState
