package piuk.blockchain.android.ui.brokerage.sell

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class SellIntent : Intent<SellModelState> {
    class CheckSellEligibility(val showLoader: Boolean) : SellIntent()
}
