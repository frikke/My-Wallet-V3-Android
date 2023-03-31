package piuk.blockchain.android.simplebuy.upsell.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface UpSellAnotherAssetIntent : Intent<UpsellAnotherAssetModelState> {

    object LoadData : UpSellAnotherAssetIntent

    object DismissUpsell : UpSellAnotherAssetIntent
}
