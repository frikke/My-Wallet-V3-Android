package com.blockchain.chrome

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.Money

data class MultiAppModelState(
    val walletModes: List<WalletMode>? = null,
    val selectedWalletMode: WalletMode? = null,
    val totalBalance: DataResource<Money> = DataResource.Loading,
    val defiBalance: DataResource<Money> = DataResource.Loading,
    val balanceRevealed: Boolean = false,
    val checkAppRating: Boolean = true
) : ModelState
