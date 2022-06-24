package piuk.blockchain.android.ui.prices

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency

data class PricesModelState(
    val isLoadingData: Boolean = false,
    val isError: Boolean = false,
    val data: MutableMap<AssetInfo, PricesItem> = mutableMapOf(),
    val filterBy: String = ""
) : ModelState

data class PricesItem(
    val assetInfo: AssetInfo,
    val fiatCurrency: Currency,
    val priceWithDelta: Prices24HrWithDelta? = null,
    val isTradingAccount: Boolean? = false
)
