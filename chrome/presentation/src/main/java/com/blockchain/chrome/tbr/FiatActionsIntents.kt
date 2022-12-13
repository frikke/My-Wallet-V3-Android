package com.blockchain.chrome.tbr

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface FiatActionsIntents : Intent<FiatActionsModelState> {
    data class Deposit(
        val account: FiatAccount,
        val action: AssetAction,
        val shouldLaunchBankLinkTransfer: Boolean,
        val shouldSkipQuestionnaire: Boolean = false
    ) : FiatActionsIntents

    data class RestartDeposit(
        val action: AssetAction? = null,
        val shouldLaunchBankLinkTransfer: Boolean,
        val shouldSkipQuestionnaire: Boolean = false
    ) : FiatActionsIntents

    object WireTransferAccountDetails : FiatActionsIntents
}
