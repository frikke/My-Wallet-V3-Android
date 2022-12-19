package com.blockchain.home.presentation.fiat.actions

import com.blockchain.coincore.AssetAction

sealed interface FiatActionRequest {
    data class Restart(
        val action: AssetAction? = null,
        val shouldLaunchBankLinkTransfer: Boolean,
        val shouldSkipQuestionnaire: Boolean = false
    ) : FiatActionRequest

    object WireTransferAccountDetails : FiatActionRequest
}
