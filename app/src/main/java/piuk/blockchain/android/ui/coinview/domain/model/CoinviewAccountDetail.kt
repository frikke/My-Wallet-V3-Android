package piuk.blockchain.android.ui.coinview.domain.model

import com.blockchain.coincore.BlockchainAccount
import info.blockchain.balance.Money

class CoinviewAccountDetail(
    val account: BlockchainAccount,
    val balance: Money,
    val isAvailable: Boolean,
    val isDefault: Boolean = false
)
