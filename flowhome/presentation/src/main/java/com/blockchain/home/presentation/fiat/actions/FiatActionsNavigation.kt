package com.blockchain.home.presentation.fiat.actions

import androidx.compose.runtime.Stable
import com.blockchain.coincore.FiatAccount

@Stable
interface FiatActionsNavigation {
    fun wireTransferDetail(account: FiatAccount)
}
