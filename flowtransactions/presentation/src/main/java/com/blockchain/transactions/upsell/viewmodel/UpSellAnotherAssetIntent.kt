package com.blockchain.transactions.upsell.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface UpSellAnotherAssetIntent : Intent<UpsellAnotherAssetModelState> {

    object LoadData : UpSellAnotherAssetIntent

    object DismissUpsell : UpSellAnotherAssetIntent
}
