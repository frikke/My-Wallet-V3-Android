package com.blockchain.home.presentation.fiat.fundsdetail

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface FiatFundsDetailIntent : Intent<FiatFundsDetailModelState> {
    object LoadData : FiatFundsDetailIntent
}
