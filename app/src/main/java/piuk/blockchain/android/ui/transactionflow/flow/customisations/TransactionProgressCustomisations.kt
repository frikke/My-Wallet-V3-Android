package piuk.blockchain.android.ui.transactionflow.flow.customisations

import androidx.annotation.DrawableRes
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState

interface TransactionProgressCustomisations {
    fun transactionProgressTitle(state: TransactionState): String
    fun transactionProgressMessage(state: TransactionState): String
    fun transactionCompleteTitle(state: TransactionState): String
    fun transactionCompleteMessage(state: TransactionState): String
    fun transactionCompleteIcon(state: TransactionState): Int
    fun transactionProgressStandardIcon(state: TransactionState): Int? // Return null to use asset icon
    fun transactionProgressExceptionTitle(state: TransactionState): String
    fun transactionProgressExceptionDescription(state: TransactionState): String
    fun transactionProgressExceptionIcon(state: TransactionState): ErrorStateIcon
}

sealed class ErrorStateIcon {
    class RemoteIconWithStatus(val iconUrl: String, val statusIconUrl: String) : ErrorStateIcon()
    class RemoteIcon(val iconUrl: String) : ErrorStateIcon()
    class Local(@DrawableRes val resourceId: Int) : ErrorStateIcon()
}
