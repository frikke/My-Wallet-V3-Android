package com.blockchain.transactions.receive.accounts

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.presentation.analytics.TxFlowAnalyticsAccountType

sealed interface ReceiveAccountsNavigation : NavigationEvent {
    data class Detail(
        val accountType: TxFlowAnalyticsAccountType,
        val networkTicker: String
    ) : ReceiveAccountsNavigation

    object KycUpgrade : ReceiveAccountsNavigation
}
