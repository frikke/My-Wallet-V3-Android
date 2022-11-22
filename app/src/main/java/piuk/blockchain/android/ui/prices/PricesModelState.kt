package piuk.blockchain.android.ui.prices

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import piuk.blockchain.android.ui.prices.presentation.PricesFilter

data class PricesModelState(
    val isLoadingData: Boolean = false,
    val isError: Boolean = false,
    val fiatCurrency: Currency? = null,
    val filters: List<PricesFilter> = emptyList(),
    val tradableCurrencies: List<String>,
    val data: List<PricesItem> = listOf(),
    val queryBy: String = "",
    val filterBy: PricesFilter = PricesFilter.All
) : ModelState

data class PricesItem(
    val assetInfo: AssetInfo,
    val hasError: Boolean,
    val currency: Currency,
    val priceWithDelta: Prices24HrWithDelta? = null,
    val isTradingAccount: Boolean? = false
)
