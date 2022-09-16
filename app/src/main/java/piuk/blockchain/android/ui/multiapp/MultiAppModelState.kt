package piuk.blockchain.android.ui.multiapp

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.walletmode.WalletMode

data class MultiAppModelState(
    val walletModes: List<WalletMode>,
    val selectedWalletMode: WalletMode,
) : ModelState
