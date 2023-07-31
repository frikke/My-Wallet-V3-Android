package com.blockchain.home.presentation.fiat.fundsdetail

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface FiatFundsDetailIntent : Intent<FiatFundsDetailModelState> {
    object LoadData : FiatFundsDetailIntent

    data class FiatAction(
        val account: FiatAccount,
        val action: AssetAction
    ) : FiatFundsDetailIntent
}
