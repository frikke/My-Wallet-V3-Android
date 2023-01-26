package piuk.blockchain.android.ui.transactionflow.flow.customisations

import android.content.Context
import android.widget.FrameLayout
import com.blockchain.coincore.AssetAction
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget

interface TargetSelectionCustomisations {
    fun selectTargetAddressTitle(state: TransactionState): String
    fun selectTargetAddressInputHint(state: TransactionState): String
    fun selectTargetAddressInputWarning(action: AssetAction, currency: Currency, coinNetwork: CoinNetwork): String
    fun selectTargetAddressTitlePick(state: TransactionState): String
    fun selectTargetShouldShowInputWarning(state: TransactionState): Boolean
    fun selectTargetShouldShowTargetPickTitle(state: TransactionState): Boolean
    fun selectTargetNoAddressMessageText(state: TransactionState): String?
    fun selectTargetShowManualEnterAddress(state: TransactionState): Boolean
    fun selectTargetShouldShowSubtitle(state: TransactionState): Boolean
    fun selectTargetSubtitle(state: TransactionState): String
    fun selectTargetAddressWalletsCta(state: TransactionState): String
    fun selectTargetSourceLabel(state: TransactionState): String
    fun selectTargetDestinationLabel(state: TransactionState): String
    fun selectTargetStatusDecorator(state: TransactionState, walletMode: WalletMode): StatusDecorator
    fun selectTargetAccountTitle(state: TransactionState): String
    fun selectTargetAccountDescription(state: TransactionState): String
    fun enterTargetAddressFragmentState(state: TransactionState): TargetAddressSheetState
    fun issueFlashMessage(state: TransactionState, input: CurrencyType?): String
    fun installAddressSheetSource(ctx: Context, frame: FrameLayout, state: TransactionState): TxFlowWidget
    fun sendToDomainCardTitle(state: TransactionState): String
    fun sendToDomainCardDescription(state: TransactionState): String
    fun shouldShowSendToDomainBanner(state: TransactionState): Boolean
    fun selectTargetNetworkDescription(state: TransactionState): String
    fun shouldShowSelectTargetNetworkDescription(state: TransactionState): Boolean
}
