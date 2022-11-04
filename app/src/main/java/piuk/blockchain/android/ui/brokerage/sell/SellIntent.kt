package piuk.blockchain.android.ui.brokerage.sell

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.AssetInfo

sealed class SellIntent : Intent<SellModelState> {
    class CheckSellEligibility(val showLoader: Boolean) : SellIntent()
    class LoadSupportedAccounts(val supportedAssets: List<AssetInfo>) : SellIntent()
    class FilterAccounts(val searchTerm: String) : SellIntent()
}
