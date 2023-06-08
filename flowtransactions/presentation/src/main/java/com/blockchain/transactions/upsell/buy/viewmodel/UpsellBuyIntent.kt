package com.blockchain.transactions.upsell.buy.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface UpsellBuyIntent : Intent<UpsellBuyModelState> {

    object LoadData : UpsellBuyIntent

    object DismissUpsell : UpsellBuyIntent
}
