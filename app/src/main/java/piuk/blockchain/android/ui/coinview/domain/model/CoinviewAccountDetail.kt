package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.StateAwareAction
import info.blockchain.balance.Money

class CoinviewAccountDetail(
    val account: BlockchainAccount,
    val balance: Money,
    val isDefault: Boolean = false
)
