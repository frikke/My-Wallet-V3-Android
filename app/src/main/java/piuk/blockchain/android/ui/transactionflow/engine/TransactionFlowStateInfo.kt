package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.coincore.AssetAction
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.price.ExchangeRate
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

interface TransactionFlowStateInfo {
    val errorState: TransactionErrorState
    val limits: TxLimits?
    val action: AssetAction
    val amount: Money
    val fiatRate: ExchangeRate?
    val availableBalance: Money?
    val sendingAsset: AssetInfo?
}
