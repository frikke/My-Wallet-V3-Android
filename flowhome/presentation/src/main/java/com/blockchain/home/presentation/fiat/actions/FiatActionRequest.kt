package com.blockchain.home.presentation.fiat.actions

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount

sealed interface FiatActionRequest {
    data class Deposit(
        val account: FiatAccount,
        val action: AssetAction,
        val shouldLaunchBankLinkTransfer: Boolean,
        val shouldSkipQuestionnaire: Boolean = false
    ) : FiatActionRequest

    data class RestartDeposit(
        val action: AssetAction? = null,
        val shouldLaunchBankLinkTransfer: Boolean,
        val shouldSkipQuestionnaire: Boolean = false
    ) : FiatActionRequest

    object WireTransferAccountDetails : FiatActionRequest
}
