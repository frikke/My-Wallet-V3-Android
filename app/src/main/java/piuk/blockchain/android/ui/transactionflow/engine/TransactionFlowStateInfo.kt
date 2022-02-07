package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.coincore.AssetAction
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.price.ExchangeRate
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.Currency
import info.blockchain.balance.Money

interface TransactionFlowStateInfo {
    val errorState: TransactionErrorState
    val sourceAccountType: AssetCategory
    val limits: TxLimits
    val action: AssetAction
    val amount: Money
    val fiatRate: ExchangeRate?
    val availableBalance: Money?
    val sendingAsset: Currency?
    val receivingAsset: Currency
}
