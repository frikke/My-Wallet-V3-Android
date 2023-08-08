package com.blockchain.transactions.receive.navigation

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination

sealed class ReceiveDestination(
    override val route: String
) : ComposeNavigationDestination {
    object Accounts : ReceiveDestination("ReceiveAccounts")
    object AccountDetail : ReceiveDestination("ReceiveAccountDetail")
    object KycUpgrade : ReceiveDestination("ReceiveKycUpgrade")
}
