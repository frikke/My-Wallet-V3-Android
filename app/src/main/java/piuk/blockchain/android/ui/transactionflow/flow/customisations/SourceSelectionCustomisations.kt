package piuk.blockchain.android.ui.transactionflow.flow.customisations

import com.blockchain.coincore.AssetAction
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState

interface SourceSelectionCustomisations {
    fun selectSourceAddressTitle(state: TransactionState): String
    fun selectSourceAccountTitle(state: TransactionState): String
    fun selectSourceShouldShowSubtitle(state: TransactionState): Boolean
    fun selectSourceAccountSubtitle(state: TransactionState): String
    fun selectSourceShouldShowAddNew(state: TransactionState): Boolean
    fun selectSourceShouldShowDepositTooltip(state: TransactionState): Boolean
    fun sourceAccountSelectionStatusDecorator(state: TransactionState): StatusDecorator
    fun getLinkingSourceForAction(state: TransactionState): BankAuthSource
    fun selectSourceShouldHaveSearch(action: AssetAction): Boolean
    fun shouldShowSourceAccountWalletsSwitch(action: AssetAction): Boolean
}
