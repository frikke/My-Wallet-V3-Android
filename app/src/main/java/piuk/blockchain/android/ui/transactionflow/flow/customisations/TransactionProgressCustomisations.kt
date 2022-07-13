package piuk.blockchain.android.ui.transactionflow.flow.customisations

import androidx.annotation.DrawableRes
import com.blockchain.domain.common.model.ServerErrorAction
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
    fun transactionProgressExceptionActions(state: TransactionState): List<ServerErrorAction>
    fun transactionSettlementExceptionAction(state: TransactionState): SettlementErrorStateAction
}

sealed class ErrorStateIcon {
    class RemoteIconWithStatus(val iconUrl: String, val statusIconUrl: String) : ErrorStateIcon()
    class RemoteIcon(val iconUrl: String) : ErrorStateIcon()
    class Local(@DrawableRes val resourceId: Int) : ErrorStateIcon()
}

sealed class SettlementErrorStateAction {
    object None : SettlementErrorStateAction()
    class RelinkBank(val message: String, val bankAccountId: String) : SettlementErrorStateAction()
}
