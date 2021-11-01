package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.coincore.AssetAction
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

interface TransactionFlowStateInfo {
    val errorState: TransactionErrorState
    val minLimit: Money?
    val maxLimit: Money?
    val action: AssetAction
    val amount: Money
    val availableBalance: Money?
    val sendingAsset: AssetInfo?
}