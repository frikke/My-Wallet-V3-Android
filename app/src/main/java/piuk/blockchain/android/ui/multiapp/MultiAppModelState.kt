package piuk.blockchain.android.ui.multiapp

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.Money

data class MultiAppModelState(
    val walletModes: List<WalletMode> = WalletMode.values().toList().minus(WalletMode.UNIVERSAL),
    val selectedWalletMode: WalletMode,
    val totalBalance: DataResource<Money> = DataResource.Loading
) : ModelState
